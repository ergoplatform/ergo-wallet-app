package org.ergoplatform.android.wallet

import android.animation.LayoutTransition
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentWalletDetailsBinding
import org.ergoplatform.android.nanoErgsToErgs
import org.ergoplatform.android.tokens.inflateAndBindTokenView
import org.ergoplatform.android.ui.navigateSafe

class WalletDetailsFragment : Fragment() {

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
            if (!nodeConnector.refreshByUser(requireContext())) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        nodeConnector.isRefreshing.observe(viewLifecycleOwner, { isRefreshing ->
            if (!isRefreshing) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        })

        // Set button listeners
        binding.cardTransactions.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    StageConstants.EXPLORER_WEB_ADDRESS + "en/addresses/" +
                            walletDetailsViewModel.wallet!!.getDerivedAddress(
                                walletDetailsViewModel.selectedIdx ?: 0
                            )
                )
            )
            binding.root.context.startActivity(browserIntent)
        }

        binding.buttonConfigAddresses.setOnClickListener {
            findNavController().navigateSafe(
                WalletDetailsFragmentDirections.actionNavigationWalletDetailsToWalletAddressesFragment(
                    walletDetailsViewModel.wallet!!.walletConfig.id
                )
            )
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

    private fun addressChanged(address: String?) {
        // The selected address changed. It is null for "all addresses"

        // wallet is safely non-null here
        val wallet = walletDetailsViewModel.wallet!!

        binding.walletName.text = wallet.walletConfig.displayName

        // fill address
        //TODO insert label here and ellipsize end for labels
        binding.publicAddress.text =
            address ?: getString(R.string.label_all_addresses, wallet.getNumOfAddresses())

        // fill balances
        val ergoAmount = nanoErgsToErgs(
            address?.let { wallet.getStateForAddress(address) }?.balance
                ?: wallet.getBalanceForAllAddresses()
        )
        binding.walletBalance.amount = ergoAmount

        val unconfirmed = address?.let { wallet.getStateForAddress(address) }?.unconfirmedBalance
            ?: wallet.getUnconfirmedBalanceForAllAddresses()
        binding.walletUnconfirmed.amount = nanoErgsToErgs(unconfirmed)
        binding.walletUnconfirmed.visibility = if (unconfirmed == 0L) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        // Fill fiat value
        val nodeConnector = NodeConnector.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value ?: 0f
        if (ergoPrice == 0f) {
            binding.walletFiat.visibility = View.GONE
        } else {
            binding.walletFiat.visibility = View.VISIBLE
            binding.walletFiat.amount = ergoPrice * binding.walletBalance.amount
            binding.walletFiat.setSymbol(nodeConnector.fiatCurrency.toUpperCase())
        }

        // tokens
        binding.cardviewTokens.visibility = if (wallet.tokens.size > 0) View.VISIBLE else View.GONE
        val tokensList = address?.let { wallet.getTokensForAddress(address) }
            ?: wallet.getTokensForAllAddresses()
        binding.walletTokenNum.text = tokensList.size.toString()

        binding.walletTokenEntries.apply {
            removeAllViews()
            if (wallet.walletConfig.unfoldTokens) {
                tokensList.forEach { inflateAndBindTokenView(it, this, layoutInflater) }
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
        binding.layoutBalances.layoutTransition = LayoutTransition()
        binding.layoutTokens.layoutTransition = LayoutTransition()
        binding.layoutTokens.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutOuter.layoutTransition = LayoutTransition()
        binding.layoutOuter.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}