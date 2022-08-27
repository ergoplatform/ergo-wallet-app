package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.AppDatabase
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
import org.ergoplatform.wallet.isReadOnly

abstract class SubmitTransactionFragment : AbstractAuthenticationFragment(),
    AddressChooserCallback {

    abstract val viewModel: SubmitTransactionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = this.viewModel
        viewModel.lockInterface.observe(viewLifecycleOwner) { locked ->
            if (locked)
                ProgressBottomSheetDialogFragment.showProgressDialog(childFragmentManager)
            else
                ProgressBottomSheetDialogFragment.dismissProgressDialog(childFragmentManager)
        }
        viewModel.txWorkDoneLiveData.observe(viewLifecycleOwner) { result ->
            if (!result.success) {
                val errorMsgPrefix = if (result is PromptSigningResult)
                    R.string.error_prepare_transaction
                else R.string.error_send_transaction

                if (result.errorMsg != null) {
                    showDialogWithCopyOption(
                        requireContext(),
                        getString(errorMsgPrefix) + "\n\n" + result.errorMsg
                                + "\n\n" + getString(R.string.error_use_other_node)
                    )
                } else {
                    Snackbar.make(
                        requireView(),
                        errorMsgPrefix,
                        Snackbar.LENGTH_LONG
                    ).setAnchorView(R.id.nav_view).show()
                }
            } else if (result is PromptSigningResult) {
                // if this is a prompt signing result, switch to prompt signing dialog
                SigningPromptDialogFragment().show(childFragmentManager, null)
            }
        }

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

    override val authenticationWalletConfig: WalletConfig?
        get() = viewModel.uiLogic.wallet?.walletConfig

    override fun startAuthFlow() {
        if (authenticationWalletConfig?.isReadOnly() != false) {
            // we have a read only wallet here, let's go to cold wallet support mode
            val context = requireContext()
            viewModel.uiLogic.startColdWalletPayment(
                Preferences(context),
                AndroidStringProvider(context)
            )
        } else {
            super.startAuthFlow()
        }
    }

    override fun proceedFromAuthFlow(secrets: SigningSecrets) {
        val context = requireContext()
        viewModel.uiLogic.startPaymentWithMnemonicAsync(
            secrets,
            Preferences(context),
            AndroidStringProvider(context),
            AppDatabase.getInstance(context)
        )
    }
}