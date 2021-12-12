package org.ergoplatform.android.wallet

import android.animation.LayoutTransition
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
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
import org.ergoplatform.wallet.*
import org.ergoplatform.wallet.addresses.getAddressLabel

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

        walletDetailsViewModel.address.observe(viewLifecycleOwner, { addressChanged(it) })

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

    private fun addressChanged(address: String?) {
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
        if (address != null) {
            val addressDbEntity =
                wallet.getSortedDerivedAddressesList().find { it.publicAddress.equals(address) }
            binding.addressLabel.text =
                addressDbEntity?.getAddressLabel(AndroidStringProvider(requireContext()))
        } else {
            binding.addressLabel.text =
                getString(R.string.label_all_addresses, wallet.getNumOfAddresses())
        }

        // fill balances
        val addressState = address?.let { wallet.getStateForAddress(address) }
        val ergoAmount = ErgoAmount(
            addressState?.balance
                ?: wallet.getBalanceForAllAddresses()
        )
        binding.walletBalance.amount = ergoAmount.toDouble()

        val unconfirmed = addressState?.unconfirmedBalance
            ?: wallet.getUnconfirmedBalanceForAllAddresses()
        binding.walletUnconfirmed.amount = ErgoAmount(unconfirmed).toDouble()
        binding.walletUnconfirmed.visibility = if (unconfirmed == 0L) View.GONE else View.VISIBLE
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
        val tokensList = (address?.let { wallet.getTokensForAddress(address) }
            ?: wallet.getTokensForAllAddresses()).sortedBy { it.name?.lowercase() }
        binding.cardviewTokens.visibility = if (tokensList.size > 0) View.VISIBLE else View.GONE
        binding.walletTokenNum.text = tokensList.size.toString()

        binding.walletTokenEntries.apply {
            removeAllViews()
            if (wallet.walletConfig.unfoldTokens) {
                tokensList.forEach { inflateAndBindDetailedTokenEntryView(it, this, layoutInflater) }
            }
        }

        binding.unfoldTokens.setImageResource(
            if (wallet.walletConfig.unfoldTokens)
                R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_down_24
        )
        binding.cardviewTokens.setOnClickListener {
            GlobalScope.launch {
                AppDatabase.getInstance(it.context).walletDao().updateWalletTokensUnfold(
                    wallet.walletConfig.id,
                    !wallet.walletConfig.unfoldTokens
                )
                // we don't need to update UI here - the DB change will trigger a rebind of the card
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}