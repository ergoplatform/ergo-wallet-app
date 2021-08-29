package org.ergoplatform.android.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentWalletDetailsBinding
import org.ergoplatform.android.nanoErgsToErgs

class WalletDetailsFragment : Fragment() {

    private lateinit var walletDetailsViewModel: WalletDetailsViewModel

    private var _binding: FragmentWalletDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: WalletDetailsFragmentArgs by navArgs()

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
        val tokensList = address?.let { wallet.getTokensForAddress(address) } ?: wallet.getTokensForAllAddresses()
        binding.walletTokenNum.text = tokensList.size.toString()

        // TODO fill token entries
        binding.walletTokenEntries.removeAllViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}