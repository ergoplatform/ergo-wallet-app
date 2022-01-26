package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.ProgressBottomSheetDialogFragment
import org.ergoplatform.android.ui.showDialogWithCopyOption
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.PromptSigningResult

abstract class SubmitTransactionFragment : AbstractAuthenticationFragment(),
    AddressChooserCallback {

    abstract val viewModel: SubmitTransactionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = this.viewModel
        viewModel.lockInterface.observe(viewLifecycleOwner, {
            if (it)
                ProgressBottomSheetDialogFragment.showProgressDialog(childFragmentManager)
            else
                ProgressBottomSheetDialogFragment.dismissProgressDialog(childFragmentManager)
        })
        viewModel.txWorkDoneLiveData.observe(viewLifecycleOwner, {
            if (!it.success) {
                val snackbar = Snackbar.make(
                    requireView(),
                    if (it is PromptSigningResult)
                        R.string.error_prepare_transaction
                    else R.string.error_send_transaction,
                    Snackbar.LENGTH_LONG
                )
                it.errorMsg?.let { errorMsg ->
                    snackbar.setAction(
                        R.string.label_details
                    ) {
                        showDialogWithCopyOption(requireContext(), errorMsg)
                    }
                }
                snackbar.setAnchorView(R.id.nav_view).show()
            } else if (it is PromptSigningResult) {
                // if this is a prompt signing result, switch to prompt signing dialog
                SigningPromptDialogFragment().show(childFragmentManager, null)
            }
        })

    }

    fun showChooseAddressList(addShowAllEntry: Boolean) {
        viewModel.uiLogic.wallet?.let { wallet ->
            ChooseAddressListDialogFragment.newInstance(
                wallet.walletConfig.id, addShowAllEntry
            ).show(childFragmentManager, null)
        }
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        viewModel.uiLogic.derivedAddressIdx = addressDerivationIdx
    }

    override fun startAuthFlow(walletConfig: WalletConfig) {
        if (walletConfig.secretStorage == null) {
            // we have a read only wallet here, let's go to cold wallet support mode
            val context = requireContext()
            viewModel.uiLogic.startColdWalletPayment(
                Preferences(context),
                AndroidStringProvider(context)
            )
        } else {
            super.startAuthFlow(walletConfig)
        }
    }

    override fun proceedAuthFlowWithPassword(password: String): Boolean {
        return viewModel.startPaymentWithPassword(password, requireContext())
    }

    override fun proceedAuthFlowFromBiometrics() {
        context?.let { viewModel.startPaymentUserAuth(it) }
    }
}