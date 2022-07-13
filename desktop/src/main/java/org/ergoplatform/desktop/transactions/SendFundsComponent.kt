package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.uilogic.STRING_ZXING_BUTTON_OK
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic

class SendFundsComponent(
    componentContext: ComponentContext,
    navHost: NavHostComponent,
    private val walletConfig: WalletConfig,
    private val derivationIdx: Int = -1,
    private val paymentRequest: String? = null,
) : SubmitTransactionComponent(componentContext, navHost) {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_BUTTON_SEND)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton({
                router.push(ScreenConfig.QrCodeScanner { qrCode ->
                    onQrCodeScanned(qrCode)
                })
            }) {
                Icon(Icons.Default.QrCodeScanner, null)
            }
        }

    private val walletAddressState = mutableStateOf<WalletAddress?>(null)
    private val recipientAddress = mutableStateOf(TextFieldValue())
    private val recipientError = mutableStateOf(false)
    private val amountError = mutableStateOf(false)
    private val amountsChangedCount = mutableStateOf(0)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        // TODO address chooser

        SendFundsScreen(
            walletConfig,
            walletAddressState.value,
            recipientAddress,
            amountToSend,
            amountsChangedCount.value,
            recipientError,
            amountError,
            uiLogic,
            onChooseToken = {

            },
            onSendClicked = { checkAndStartPayment() },
        )

        SubmitTransactionOverlays()
    }

    private fun onQrCodeScanned(qrCode: String) {
        TODO("Not yet implemented")
    }

    private fun checkAndStartPayment() {
        val checkResponse = uiLogic.checkCanMakePayment(Application.prefs)

        recipientError.value = checkResponse.receiverError
        amountError.value = checkResponse.amountError
        // TODO inputMessage.setHasError(checkResponse.messageError)
        // TODO focus
        if (checkResponse.tokenError) {
            // TODO tokensError.setHiddenAnimated(false)
        }
        if (checkResponse.messageError) {
            // TODO
        }

        if (checkResponse.canPay) {
            startPayment()
        }
    }

    override val uiLogic = object : SendFundsUiLogic() {
        override fun notifyTokensChosenChanged() {
            // TODO
        }

        override fun notifyAmountsChanged() {
            amountsChangedCount.value = amountsChangedCount.value + 1
        }

        override fun notifyBalanceChanged() {
            notifyAmountsChanged()
        }

        override fun showErrorMessage(message: String) {
            navHost.dialogHandler.showDialog(
                MosaikDialog(
                    message, Application.texts.getString(STRING_ZXING_BUTTON_OK),
                    null, null, null
                )
            )
        }

        override fun onNotifySuggestedFees() {
            TODO("Not yet implemented")
        }

        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyWalletStateLoaded() {
            notifyAmountsChanged()
        }

        override fun notifyDerivedAddressChanged() {
            walletAddressState.value = derivedAddress
        }

        override fun notifyUiLocked(locked: Boolean) {
            navHost.lockScreen.value = locked
        }

        override fun notifyHasTxId(txId: String) {
            TODO("Not yet implemented")
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            TODO("Not yet implemented")
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            TODO("Not yet implemented")
        }

    }.apply {
        initWallet(
            Application.database, ApiServiceManager.getOrInit(Application.prefs),
            walletConfig.id, derivationIdx, paymentRequest
        )
    }

    private val amountToSend = mutableStateOf(TextFieldValue(uiLogic.inputAmountString))

}

