package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentErgoPaySigningBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.getSeverityDrawableResId
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.openStorePage
import org.ergoplatform.android.wallet.ChooseWalletListBottomSheetDialog
import org.ergoplatform.android.wallet.WalletChooserCallback
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getNumOfAddresses

class ErgoPaySigningFragment : SubmitTransactionFragment(), WalletChooserCallback {
    private var _binding: FragmentErgoPaySigningBinding? = null
    private val binding get() = _binding!!

    private val args: ErgoPaySigningFragmentArgs by navArgs()

    override val viewModel: ErgoPaySigningViewModel
        get() = ViewModelProvider(this).get(ErgoPaySigningViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErgoPaySigningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = this.viewModel
        val context = requireContext()
        viewModel.uiLogic.init(
            args.request,
            args.walletId,
            args.derivationIdx,
            RoomWalletDbProvider(AppDatabase.getInstance(context)),
            Preferences(context),
            AndroidStringProvider(context)
        )

        viewModel.uiStateRefresh.observe(viewLifecycleOwner) { state ->
            binding.layoutTransactionInfo.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION)
            binding.layoutProgress.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.FETCH_DATA)
            binding.layoutDoneInfo.visibility =
                visibleWhen(state, ErgoPaySigningUiLogic.State.DONE)
            binding.layoutChooseAddress.visibility =
                if (state == ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS ||
                    state == ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET
                ) View.VISIBLE else View.GONE

            when (state) {
                ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> {
                    binding.labelChooseWalletOrAddress.setText(R.string.label_ergo_pay_choose_address)
                    binding.buttonChooseAddress.setText(R.string.title_choose_address)
                }
                ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET -> {
                    binding.labelChooseWalletOrAddress.setText(R.string.label_ergo_pay_choose_wallet)
                    binding.buttonChooseAddress.setText(R.string.title_choose_wallet)
                }
                ErgoPaySigningUiLogic.State.FETCH_DATA -> showFetchData()
                ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> showTransactionInfo()
                ErgoPaySigningUiLogic.State.DONE -> showDoneInfo()
                null -> {} // impossible
            }
        }

        viewModel.addressChosen.observe(viewLifecycleOwner) {
            val walletLabel = viewModel.uiLogic.wallet?.walletConfig?.displayName ?: ""
            val addressLabel =
                it?.getAddressLabel(AndroidStringProvider(requireContext()))
                    ?: getString(
                        R.string.label_all_addresses,
                        viewModel.uiLogic.wallet?.getNumOfAddresses()
                    )
            binding.addressLabel.text =
                getString(R.string.label_sign_with, addressLabel, walletLabel)
        }

        // Click listeners
        binding.transactionInfo.buttonSignTx.setOnClickListener {
            startAuthFlow()
        }
        binding.buttonDismiss.setOnClickListener {
            if (args.closeApp) {
                requireActivity().finish()
            } else {
                findNavController().popBackStack()
            }
        }
        binding.buttonRetry.setOnClickListener {
            startReloadFromDapp()
        }
        binding.buttonChooseAddress.setOnClickListener {
            showAddressOrWalletChooser()
        }
        binding.buttonRate.setOnClickListener {
            openStorePage(requireContext())
        }

        if (args.closeApp) {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                requireActivity().finish()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_ergopay, menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_reload).isEnabled = viewModel.uiLogic.canReloadFromDapp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_reload) {
            startReloadFromDapp()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startReloadFromDapp() {
        val context = requireContext()
        viewModel.uiLogic.reloadFromDapp(
            Preferences(context),
            AndroidStringProvider(context),
            AppDatabase.getInstance(context).walletDbProvider
        )
    }

    private fun showAddressOrWalletChooser() {
        val uiLogic = viewModel.uiLogic
        if (uiLogic.wallet != null) {
            showChooseAddressList(false)
        } else {
            ChooseWalletListBottomSheetDialog().show(childFragmentManager, null)
        }
    }

    override fun onWalletChosen(walletConfig: WalletConfig) {
        val context = requireContext()
        viewModel.uiLogic.setWalletId(
            walletConfig.id,
            Preferences(context),
            AndroidStringProvider(context),
            AppDatabase.getInstance(context).walletDbProvider
        )
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        super.onAddressChosen(addressDerivationIdx)
        val uiLogic = viewModel.uiLogic
        // retry the request - can't be called within uiLogic because the context is needed
        val context = requireContext()
        uiLogic.derivedAddressIdChanged(
            Preferences(context),
            AndroidStringProvider(context),
            AppDatabase.getInstance(context).walletDbProvider,
        )
    }

    private fun visibleWhen(
        state: ErgoPaySigningUiLogic.State?,
        visibleWhen: ErgoPaySigningUiLogic.State
    ) =
        if (state == visibleWhen) View.VISIBLE else View.GONE

    private fun showFetchData() {
        // nothing special to do yet
    }

    private fun showDoneInfo() {
        // use normal tx done message in case of success
        val uiLogic = viewModel.uiLogic
        binding.tvMessage.text = uiLogic.getDoneMessage(AndroidStringProvider(requireContext()))
        binding.tvMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            uiLogic.getDoneSeverity().getSeverityDrawableResId(),
            0, 0
        )
        val shouldReload =
            (uiLogic.getDoneSeverity() == MessageSeverity.ERROR && uiLogic.canReloadFromDapp())
        binding.buttonDismiss.visibility = if (!shouldReload) View.VISIBLE else View.GONE
        binding.buttonRetry.visibility = if (shouldReload) View.VISIBLE else View.GONE
        val showRatingPrompt = uiLogic.showRatingPrompt()
        binding.buttonRate.visibility = if (showRatingPrompt) View.VISIBLE else View.GONE
        binding.tvRate.visibility = if (showRatingPrompt) View.VISIBLE else View.GONE

        viewModel.uiLogic.txId?.let {
            parentFragmentManager.setFragmentResult(
                ergoPayActionRequestKey, bundleOf(
                    ergoPayActionCompletedBundleKey to true
                )
            )
        }
    }

    private fun showTransactionInfo() {
        val uiLogic = viewModel.uiLogic
        binding.transactionInfo.bindTransactionInfo(
            uiLogic.transactionInfo!!.reduceBoxes(),
            { tokenId ->
                findNavController().navigateSafe(
                    ErgoPaySigningFragmentDirections.actionErgoPaySigningToTokenInformation(tokenId)
                )
            },
            layoutInflater
        )
        binding.layoutTiMessage.visibility = uiLogic.epsr?.message?.let {
            binding.tvTiMessage.text = getString(R.string.label_message_from_dapp, it)
            val severityResId =
                (uiLogic.epsr?.messageSeverity ?: MessageSeverity.NONE).getSeverityDrawableResId()
            binding.imageTiMessage.setImageResource(severityResId)
            binding.imageTiMessage.visibility = if (severityResId == 0) View.GONE else View.VISIBLE
            View.VISIBLE
        } ?: View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val ergoPayActionRequestKey = "KEY_ERGOPAY_SIGNING_FRAGMENT"
        val ergoPayActionCompletedBundleKey = "KEY_ERGOPAY_SUBMITTED_DONE"
    }
}