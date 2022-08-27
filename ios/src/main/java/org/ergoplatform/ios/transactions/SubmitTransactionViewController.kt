package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.IosStringProvider
import org.ergoplatform.ios.ui.ViewControllerWithKeyboardLayoutGuide
import org.ergoplatform.ios.ui.getAppDelegate
import org.ergoplatform.ios.ui.startAuthFlow
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.SubmitTransactionUiLogic
import org.robovm.apple.uikit.UIAlertAction
import org.robovm.apple.uikit.UIAlertActionStyle
import org.robovm.apple.uikit.UIAlertController
import org.robovm.apple.uikit.UIAlertControllerStyle

abstract class SubmitTransactionViewController : ViewControllerWithKeyboardLayoutGuide() {
    protected lateinit var texts: I18NBundle
    protected abstract val uiLogic: SubmitTransactionUiLogic

    protected open fun onAddressChosen(it: Int?) {
        uiLogic.derivedAddressIdx = it
    }

    protected fun startPayment() {
        val walletConfig = uiLogic.wallet!!.walletConfig
        val appDelegate = getAppDelegate()
        val stringProvider = IosStringProvider(appDelegate.texts)
        walletConfig.secretStorage?.let {
            startAuthFlow(walletConfig) { mnemonic ->
                uiLogic.startPaymentWithMnemonicAsync(
                    mnemonic,
                    appDelegate.prefs,
                    stringProvider,
                    appDelegate.database
                )
            }
        } ?: uiLogic.startColdWalletPayment(appDelegate.prefs, stringProvider)
    }

    protected fun showSigningPromptVc(signingPrompt: String) {
        presentViewController(
            SigningPromptViewController(signingPrompt, uiLogic), true
        ) {}
    }

    protected fun showTxResultError(txResult: TransactionResult) {
        val message =
            texts.get(
                if (txResult is PromptSigningResult) STRING_ERROR_PREPARE_TRANSACTION
                else STRING_ERROR_SEND_TRANSACTION
            ) + (txResult.errorMsg?.let { "\n\n$it" } ?: "") +
                    "\n\n" + texts.get(STRING_ERROR_USE_OTHER_NODE)
        val alertVc =
            UIAlertController(
                texts.get(STRING_BUTTON_SEND),
                message,
                UIAlertControllerStyle.Alert
            )
        alertVc.addAction(
            UIAlertAction(
                texts.get(STRING_ZXING_BUTTON_OK),
                UIAlertActionStyle.Default
            ) {})
        presentViewController(alertVc, true) {}
    }
}