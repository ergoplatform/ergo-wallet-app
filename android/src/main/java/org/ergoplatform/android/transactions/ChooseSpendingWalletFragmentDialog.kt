package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentSendFundsWalletChooserBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.wallet.addWalletChooserItemBindings
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.transactions.isErgoPaySigningRequest


/**
 * Deep link to send funds: Choose wallet to spend from
 */
class ChooseSpendingWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentSendFundsWalletChooserBinding? = null

    // This property is only valid between onCreateDialog and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendFundsWalletChooserBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val query = arguments?.getString(ARG_QUERY)

        if (query == null) {
            dismiss()
            return
        }

        if (isErgoPaySigningRequest(query)) {
            binding.grossAmount.visibility = View.GONE
            binding.textviewTo.visibility = View.GONE
            binding.receiverAddress.visibility = View.GONE
            binding.labelTitle.setText(R.string.title_ergo_pay_request)
        } else {
            val content = parsePaymentRequest(query)
            binding.receiverAddress.text = content?.address
            val amount = content?.amount ?: ErgoAmount.ZERO
            binding.grossAmount.setAmount(amount.toBigDecimal())
            binding.grossAmount.visibility = if (amount.nanoErgs > 0) View.VISIBLE else View.GONE
        }

        AppDatabase.getInstance(requireContext()).walletDao().getWalletsWithStates()
            .observe(viewLifecycleOwner) { walletList ->
                val wallets = walletList.map { it.toModel() }
                binding.listWallets.removeAllViews()

                if (wallets.size == 1) {
                    // immediately switch to send funds screen
                    navigateToNextScreen(wallets.first().walletConfig.id, query)
                }
                addWalletChooserItemBindings(
                    layoutInflater,
                    binding.listWallets,
                    wallets,
                    false
                ) { walletConfig ->
                    navigateToNextScreen(walletConfig.id, query)
                }
            }
    }

    private fun navigateToNextScreen(walletId: Int, request: String) {
        val navBuilder = NavOptions.Builder()
        val navOptions =
            navBuilder.setPopUpTo(R.id.chooseSpendingWalletFragmentDialog, true).build()

        NavHostFragment.findNavController(requireParentFragment())
            .navigateSafe(
                if (isErgoPaySigningRequest(request))
                    ChooseSpendingWalletFragmentDialogDirections.actionChooseSpendingWalletFragmentDialogToErgoPaySigningFragment(
                        request, walletId
                    )
                else
                    ChooseSpendingWalletFragmentDialogDirections.actionChooseSpendingWalletFragmentDialogToSendFundsFragment(
                        request, walletId
                    ),
                navOptions
            )
    }

    override fun onBackPressed(): Boolean {
        // Workaround for bug in Navigation component: navigating back does not navigate
        // to startDestination for dialogs. So we do this explicitely here
        NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.navigation_wallet)
        dismiss()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_QUERY = "ARG_QUERY"

        fun buildArgs(query: String): Bundle {
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            return args
        }
    }
}