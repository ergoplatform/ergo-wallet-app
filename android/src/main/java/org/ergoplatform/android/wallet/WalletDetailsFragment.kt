package org.ergoplatform.android.wallet

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryWalletTokenDetailsBinding
import org.ergoplatform.android.databinding.FragmentWalletDetailsBinding
import org.ergoplatform.android.tokens.WalletDetailsTokenEntryView
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.openUrlWithBrowser
import org.ergoplatform.android.ui.postDelayed
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.getExplorerWebUrl
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.wallet.getDerivedAddress

class WalletDetailsFragment : Fragment(), AddressChooserCallback {

    private lateinit var walletDetailsViewModel: WalletDetailsViewModel

    private var _binding: FragmentWalletDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: WalletDetailsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        walletDetailsViewModel =
            ViewModelProvider(this).get(WalletDetailsViewModel::class.java)
        walletDetailsViewModel.init(requireContext(), args.walletId)
        _binding = FragmentWalletDetailsBinding.inflate(layoutInflater, container, false)

        walletDetailsViewModel.address.observe(viewLifecycleOwner) { onDataChanged() }
        walletDetailsViewModel.tokenInfo.observe(viewLifecycleOwner) { onTokenInfoChanged(it) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nodeConnector = WalletStateSyncManager.getInstance()
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!walletDetailsViewModel.uiLogic.refreshByUser(
                    Preferences(requireContext()),
                    AppDatabase.getInstance(requireContext())
                )
            ) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                nodeConnector.isRefreshing.collect { isRefreshing ->
                    if (!isRefreshing) {
                        binding.progressBar.hide()
                        binding.swipeRefreshLayout.isRefreshing = false
                    } else {
                        binding.progressBar.show()
                    }
                }
            }
        }

        // Set button listeners
        binding.cardTransactions.setOnClickListener {
            openUrlWithBrowser(
                binding.root.context,
                getExplorerWebUrl() + "en/addresses/" +
                        walletDetailsViewModel.wallet!!.getDerivedAddress(
                            walletDetailsViewModel.selectedIdx ?: 0
                        )
            )
        }

        binding.buttonConfigAddresses.setOnClickListener {
            findNavController().navigateSafe(
                WalletDetailsFragmentDirections.actionNavigationWalletDetailsToWalletAddressesFragment(
                    walletDetailsViewModel.wallet!!.walletConfig.id
                )
            )
        }

        binding.buttonReceive.setOnClickListener {
            findNavController().navigateSafe(
                WalletDetailsFragmentDirections.actionNavigationWalletDetailsToReceiveToWalletFragment(
                    walletDetailsViewModel.wallet!!.walletConfig.id,
                    walletDetailsViewModel.selectedIdx ?: 0
                )
            )
        }

        binding.buttonSend.setOnClickListener {
            findNavController().navigateSafe(
                WalletDetailsFragmentDirections.actionNavigationWalletDetailsToSendFundsFragment(
                    walletDetailsViewModel.wallet!!.walletConfig.id,
                    walletDetailsViewModel.selectedIdx ?: -1
                )
            )
        }

        binding.layoutAddressLabels.setOnClickListener {
            ChooseAddressListDialogFragment.newInstance(
                walletDetailsViewModel.wallet!!.walletConfig.id,
                true
            ).show(childFragmentManager, null)
        }

        binding.buttonScan.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }

        // enable layout change animations after a short wait time
        postDelayed(500) { enableLayoutChangeAnimations() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_wallet_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_settings) {
            findNavController().navigateSafe(
                WalletDetailsFragmentDirections.actionNavigationWalletDetailsToWalletConfigFragment(
                    walletDetailsViewModel.wallet!!.walletConfig.id
                )
            )

            return true
        } else
            return super.onOptionsItemSelected(item)
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        val context = requireContext()
        walletDetailsViewModel.uiLogic.newAddressIdxChosen(
            addressDerivationIdx,
            Preferences(context),
            AppDatabase.getInstance(context)
        )
    }

    private fun onDataChanged() {
        // The selected address changed. It is null for "all addresses"

        if (walletDetailsViewModel.wallet == null) {
            // wallet was deleted from config screen
            findNavController().popBackStack()
            return
        }

        // wallet is safely non-null here
        val wallet = walletDetailsViewModel.wallet!!

        binding.walletName.text = wallet.walletConfig.displayName

        // fill address or label
        binding.addressLabel.text =
            walletDetailsViewModel.uiLogic.getAddressLabel(AndroidStringProvider(requireContext()))

        // fill balances
        val ergoAmount = walletDetailsViewModel.uiLogic.getErgoBalance()
        val unconfirmed = walletDetailsViewModel.uiLogic.getUnconfirmedErgoBalance()

        binding.walletBalance.setAmount(ergoAmount.toBigDecimal())
        binding.walletUnconfirmed.setAmount(unconfirmed.toBigDecimal())
        binding.walletUnconfirmed.visibility = if (unconfirmed.isZero()) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        // Fill fiat value
        val nodeConnector = WalletStateSyncManager.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value
        if (ergoPrice == 0f) {
            binding.walletFiat.visibility = View.GONE
        } else {
            binding.walletFiat.visibility = View.VISIBLE
            binding.walletFiat.amount = ergoPrice * ergoAmount.toDouble()
            binding.walletFiat.setSymbol(nodeConnector.fiatCurrency.uppercase())
        }

        // tokens
        val tokensList = walletDetailsViewModel.uiLogic.tokensList
        binding.cardviewTokens.visibility = if (tokensList.isNotEmpty()) View.VISIBLE else View.GONE
        binding.walletTokenNum.text = tokensList.size.toString()

        binding.walletTokenEntries.apply {
            removeAllViews()
            tokensViewList.clear()
            if (wallet.walletConfig.unfoldTokens) {
                val tokenInfoMap = walletDetailsViewModel.uiLogic.tokenInformation
                tokensList.forEach { token ->
                    val itemBinding =
                        EntryWalletTokenDetailsBinding.inflate(
                            layoutInflater,
                            this,
                            true
                        )
                    val walletDetailsView = WalletDetailsTokenEntryView(itemBinding, token)
                    walletDetailsView.bind(tokenInfoMap[token.tokenId!!])
                    tokensViewList.add(walletDetailsView)

                    walletDetailsView.binding.root.setOnClickListener {
                        findNavController().navigateSafe(
                            WalletDetailsFragmentDirections.actionNavigationWalletDetailsToTokenInformationDialogFragment(
                                token.tokenId!!
                            ).setAmount(token.amount ?: 0)
                        )
                    }
                }
                val ctx = requireContext()
                walletDetailsViewModel.uiLogic.gatherTokenInformation(
                    AppDatabase.getInstance(ctx).tokenDbProvider,
                    ErgoApiService.getOrInit(Preferences(ctx))
                )
            }
        }

        binding.unfoldTokens.setImageResource(
            if (wallet.walletConfig.unfoldTokens)
                R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_down_24
        )
        binding.cardviewTokens.setOnClickListener {
            val context = it.context
            updateWalletTokensUnfold(context, wallet)
            // we don't need to update UI here - the DB change will trigger rebinding of the card
        }

        startRefreshWhenNeeded()
    }

    // list of token views currently in view
    private val tokensViewList = ArrayList<WalletDetailsTokenEntryView>()

    private fun onTokenInfoChanged(tokenInfoHashMap: HashMap<String, TokenInformation>) {
        // token info changed, or we have the last call from another view instance
        tokensViewList.forEach { view ->
            view.tokenId?.let { tokenId ->
                val tokenInfo = tokenInfoHashMap.get(tokenId)
                tokenInfo?.let { view.addTokenInfo(tokenInfo) }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateWalletTokensUnfold(context: Context, wallet: Wallet) {
        GlobalScope.launch {
            AppDatabase.getInstance(context).walletDao().updateWalletTokensUnfold(
                wallet.walletConfig.id,
                !wallet.walletConfig.unfoldTokens
            )
        }
    }

    private fun enableLayoutChangeAnimations() {
        // set layout change animations. they are not set in the xml to avoid animations for the first
        // time the layout is displayed
        _binding?.let { binding ->
            binding.layoutBalances.layoutTransition = LayoutTransition()
            binding.layoutTokens.layoutTransition = LayoutTransition()
            binding.layoutTokens.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            binding.layoutOuter.layoutTransition = LayoutTransition()
            binding.layoutOuter.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            binding.layoutTransactions.layoutTransition = LayoutTransition()
            binding.layoutTransactions.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    override fun onResume() {
        super.onResume()
        startRefreshWhenNeeded()
    }

    private fun startRefreshWhenNeeded() {
        val context = requireContext()
        walletDetailsViewModel.uiLogic.refreshWhenNeeded(
            Preferences(context),
            AppDatabase.getInstance(context)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { qrCodeScanned(it) }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun qrCodeScanned(qrCode: String) {
        walletDetailsViewModel.uiLogic.qrCodeScanned(
            qrCode,
            AndroidStringProvider(requireContext()),
            { data ->
                findNavController().navigate(
                    WalletDetailsFragmentDirections
                        .actionNavigationWalletDetailsToColdWalletSigningFragment(
                            data,
                            walletDetailsViewModel.wallet!!.walletConfig.id,
                        )
                )
            },
            { ergoPayRequest ->
                findNavController().navigateSafe(
                    WalletDetailsFragmentDirections.actionNavigationWalletDetailsToErgoPaySigningFragment(
                        ergoPayRequest,
                        walletDetailsViewModel.wallet!!.walletConfig.id,
                        walletDetailsViewModel.selectedIdx ?: -1
                    )
                )
            },
            { data ->
                findNavController().navigate(
                    WalletDetailsFragmentDirections
                        .actionNavigationWalletDetailsToSendFundsFragment(
                            walletDetailsViewModel.wallet!!.walletConfig.id,
                            walletDetailsViewModel.selectedIdx ?: -1
                        ).setPaymentRequest(data)
                )
            }, {
                MaterialAlertDialogBuilder(requireContext()).setMessage(it)
                    .setPositiveButton(R.string.zxing_button_ok, null)
                    .show()
            })
    }
}