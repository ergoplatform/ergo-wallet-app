package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
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
    private val recipientError = mutableStateOf(false)

    // amountToSend declared below to be able to access uiLogic
    private val amountError = mutableStateOf(false)
    private val amountsChangedCount = mutableStateOf(0)
    private val txIdState = mutableStateOf<String?>(null)
    private val addTokenDialogState = mutableStateOf(false)
    private val tokensChosenState = mutableStateOf(emptyList<String>())
    private val tokensError = mutableStateOf(false)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        if (txIdState.value != null) {
            TransactionSubmittedScreen(txIdState.value!!, router::pop)
        } else {
            SendFundsScreen(
                walletConfig,
                walletAddressState.value,
                recipientAddress,
                amountToSend,
                amountsChangedCount.value,
                recipientError,
                amountError,
                tokensChosenState.value,
                tokensError,
                uiLogic,
                onChooseToken = {
                    addTokenDialogState.value = true
                },
                onSendClicked = { checkAndStartPayment() },
                onChooseAddressClicked = { startChooseAddress(true) },
            )

            if (addTokenDialogState.value) {
                ChooseTokenListDialog(
                    remember { uiLogic.getTokensToChooseFrom() },
                    uiLogic.tokensInfo,
                    onTokenChosen = { uiLogic.newTokenChosen(it.tokenId!!) },
                    onDismissRequest = { addTokenDialogState.value = false }
                )
            }

            SubmitTransactionOverlays()
        }
    }

    private fun onQrCodeScanned(qrCode: String) {
        uiLogic.qrCodeScanned(qrCode, Application.texts,
            navigateToColdWalletSigning = { data, walletId ->
                // TODO cold wallet
            },
            navigateToErgoPaySigning = { ergoPayRequest ->
                router.push(ScreenConfig.ErgoPay(ergoPayRequest, walletConfig.id, null))
            }, setPaymentRequestDataToUi = { address, amount, message ->
                recipientAddress.value = TextFieldValue(address)
                uiLogic.receiverAddress = address
                amountToSend.value = TextFieldValue(amount?.toStringTrimTrailingZeros() ?: "")
                uiLogic.inputAmountChanged(amountToSend.value.text)
                // TODO purpose message
            })
    }

    private fun checkAndStartPayment() {
        val checkResponse = uiLogic.checkCanMakePayment(Application.prefs)

        recipientError.value = checkResponse.receiverError
        amountError.value = checkResponse.amountError
        // TODO purpose message inputMessage.setHasError(checkResponse.messageError)
        // TODO focus
        tokensError.value = checkResponse.tokenError

        if (checkResponse.messageError) {
            // TODO purpose message not implemented
        }

        if (checkResponse.canPay) {
            startPayment()
        }
    }

    override val uiLogic = object : SendFundsUiLogic() {
        override fun notifyTokensChosenChanged() {
            tokensChosenState.value = tokensChosen.keys.toList()

            getPaymentRequestWarnings(Application.texts)?.let {
                showErrorMessage(it)
            }
        }

        override fun notifyAmountsChanged() {
            amountsChangedCount.value = amountsChangedCount.value + 1
        }

        override fun notifyBalanceChanged() {
            notifyAmountsChanged()
        }

        override fun showErrorMessage(message: String) {
            navHost.showErrorDialog(message)
        }

        override fun onNotifySuggestedFees() {
            TODO("Editable fee not implemented")
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
            txIdState.value = txId
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            if (!txResult.success) {
                showErrorMessage(getTransactionResultErrorMessage(txResult))
            }
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            showSigningPrompt(signingPrompt)
        }

    }.apply {
        initWallet(
            Application.database, ApiServiceManager.getOrInit(Application.prefs),
            walletConfig.id, derivationIdx, paymentRequest
        )
    }

    private val amountToSend = mutableStateOf(TextFieldValue(uiLogic.inputAmountString))
    private val recipientAddress = mutableStateOf(TextFieldValue(uiLogic.receiverAddress))

}

