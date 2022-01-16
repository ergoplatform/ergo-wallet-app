package org.ergoplatform.android.transactions

import org.ergoplatform.android.Preferences
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.persistance.WalletConfig

abstract class SubmitTransactionFragment : AbstractAuthenticationFragment(),
    AddressChooserCallback {

    protected abstract val viewModel: SubmitTransactionViewModel

    fun showChooseAddressList() {
        viewModel.uiLogic.wallet?.let { wallet ->
            ChooseAddressListDialogFragment.newInstance(
                wallet.walletConfig.id, true
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