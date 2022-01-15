package org.ergoplatform.android.wallet

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentWalletDetailsBinding
import org.ergoplatform.android.tokens.inflateAndBindDetailedTokenEntryView
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.openUrlWithBrowser
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.getExplorerWebUrl
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

        walletDetailsViewModel.address.observe(viewLifecycleOwner) { addressChanged() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nodeConnector = NodeConnector.getInstance()
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!nodeConnector.refreshByUser(
                    Preferences(requireContext()),
                    RoomWalletDbProvider(AppDatabase.getInstance(requireContext()))
                )
            ) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                nodeConnector.isRefreshing.collect { isRefreshing ->
                    if (!isRefreshing) {
                        binding.swipeRefreshLayout.isRefreshing = false
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

        // enable layout change animations after a short wait time
        Handler(Looper.getMainLooper()).postDelayed({ enableLayoutChangeAnimations() }, 500)
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
        walletDetailsViewModel.selectedIdx = addressDerivationIdx
    }

    private fun addressChanged() {
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

        binding.walletBalance.amount = ergoAmount.toDouble()
        binding.walletUnconfirmed.amount = unconfirmed.toDouble()
        binding.walletUnconfirmed.visibility = if (unconfirmed.isZero()) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        // Fill fiat value
        val nodeConnector = NodeConnector.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value
        if (ergoPrice == 0f) {
            binding.walletFiat.visibility = View.GONE
        } else {
            binding.walletFiat.visibility = View.VISIBLE
            binding.walletFiat.amount = ergoPrice * binding.walletBalance.amount
            binding.walletFiat.setSymbol(nodeConnector.fiatCurrency.uppercase())
        }

        // tokens
        val tokensList = walletDetailsViewModel.uiLogic.getTokensList()
        binding.cardviewTokens.visibility = if (tokensList.isNotEmpty()) View.VISIBLE else View.GONE
        binding.walletTokenNum.text = tokensList.size.toString()

        binding.walletTokenEntries.apply {
            removeAllViews()
            if (wallet.walletConfig.unfoldTokens) {
                tokensList.forEach {
                    inflateAndBindDetailedTokenEntryView(
                        it,
                        this,
                        layoutInflater
                    )
                }
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
        }
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        NodeConnector.getInstance().refreshWhenNeeded(
            Preferences(context),
            RoomWalletDbProvider(AppDatabase.getInstance(context))
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}