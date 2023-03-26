package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import com.arkivanov.decompose.router.replaceCurrent
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.addressbook.AddressBookDialogStateHandler
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.uilogic.transactions.SuggestedFee
import org.ergoplatform.wallet.isMultisig
import org.ergoplatform.wallet.isReadOnly

class SendFundsComponent(
    componentContext: ComponentContext,
    private val navHost: NavHostComponent,
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
    private val editFeeDialogState = mutableStateOf(false)
    private val addressBookDialogState = AddressBookDialogStateHandler()
    private val preparedTransactionInfoState = mutableStateOf<TransactionInfo?>(null)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        if (txIdState.value != null) {
            TransactionSubmittedScreen(txIdState.value!!, router::pop)
        } else {
            SendFundsScreen(
                walletConfig,
                walletAddressState.value,
                recipientAddress,
                purposeMessage,
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
                onChooseFeeClicked = {
                    editFeeDialogState.value = true
                    uiLogic.fetchSuggestedFeeData(ApiServiceManager.getOrInit(Application.prefs))
                },
                onChooseRecipientAddress = { addressBookDialogState.showChooseAddressDialog() },
                onPurposeMessageInfoClicked = ::showPurposeMessageInfoDialog,
            )

            if (addTokenDialogState.value) {
                ChooseTokenListDialog(
                    remember { uiLogic.getTokensToChooseFrom() },
                    uiLogic.tokensInfo,
                    onTokenChosen = { uiLogic.newTokenChosen(it.tokenId!!) },
                    onDismissRequest = { addTokenDialogState.value = false }
                )
            }

            if (editFeeDialogState.value) {
                ChooseFeeDialog(
                    uiLogic,
                    editableFeeList.value,
                    onDismissRequest = { editFeeDialogState.value = false }
                )
            }

            addressBookDialogState.AddressBookDialogs(
                onChooseEntry = { addressWithLabel ->
                    recipientAddress.value = TextFieldValue(addressWithLabel.address)
                    uiLogic.receiverAddress = addressWithLabel.address
                    recipientError.value = false
                },
                componentScope()
            )

            preparedTransactionInfoState.value?.let {
                ConfirmSendFundsDialog(
                    it,
                    onDismissRequest = { preparedTransactionInfoState.value = null },
                    onConfirm = {
                        preparedTransactionInfoState.value = null
                        startPayment()
                    },
                )
            }

            SubmitTransactionOverlays()
        }
    }

    private fun onQrCodeScanned(qrCode: String) {
        uiLogic.qrCodeScanned(qrCode, Application.texts,
            navigateToColdWalletSigning = { data, walletId ->
                router.push(ScreenConfig.ColdSigning(walletId, data))
            },
            navigateToErgoPaySigning = { ergoPayRequest ->
                router.push(ScreenConfig.ErgoPay(ergoPayRequest, walletConfig.id, null))
            }, setPaymentRequestDataToUi = { address, amount, message ->
                recipientAddress.value = TextFieldValue(address)
                uiLogic.receiverAddress = address
                amountToSend.value = TextFieldValue(amount?.toStringTrimTrailingZeros() ?: "")
                uiLogic.inputAmountChanged(amountToSend.value.text)
                message?.let { purposeMessage.value = TextFieldValue(message) }
            })
    }

    private fun checkAndStartPayment() {
        val checkResponse = uiLogic.checkCanMakePayment(Application.prefs)

        recipientError.value = checkResponse.receiverError
        amountError.value = checkResponse.amountError
        // TODO focus
        tokensError.value = checkResponse.tokenError

        if (checkResponse.messageError) {
            showPurposeMessageInfoDialog(startPayment = true)
        }

        if (checkResponse.canPay) {
            if (uiLogic.wallet?.walletConfig?.isReadOnly() == false)
                uiLogic.prepareTransactionForSigning(Application.prefs, Application.texts)
            else
                startPayment()
        }
    }

    private fun showPurposeMessageInfoDialog(startPayment: Boolean = false) {
        navHost.dialogHandler.showDialog(
            MosaikDialog(
                Application.texts.getString(STRING_INFO_PURPOSE_MESSAGE),
                Application.texts.getString(STRING_INFO_PURPOSE_MESSAGE_ACCEPT),
                Application.texts.getString(STRING_INFO_PURPOSE_MESSAGE_DECLINE),
                positiveButtonClicked = {
                    Application.prefs.sendTxMessages = true
                    if (startPayment) checkAndStartPayment()
                },
                negativeButtonClicked = { Application.prefs.sendTxMessages = false },
            )
        )
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
            editableFeeList.value = suggestedFeeItems
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

        override fun notifyHasSubmittedTxId(txId: String) {
            txIdState.value = txId
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            if (!txResult.success) {
                showErrorMessage(getTransactionResultErrorMessage(txResult))
            } else if (walletConfig.isMultisig()) {
                router.replaceCurrent(ScreenConfig.MultisigTxDetail(multisigTransactionId!!))
            }
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            showSigningPrompt(signingPrompt)
        }

        override fun notifyHasPreparedTx(preparedTx: TransactionInfo) {
            preparedTransactionInfoState.value = preparedTx
        }
    }.apply {
        initWallet(
            Application.database, ApiServiceManager.getOrInit(Application.prefs),
            walletConfig.id, derivationIdx, paymentRequest
        )
    }

    private val amountToSend = mutableStateOf(TextFieldValue(uiLogic.inputAmountString))
    private val purposeMessage = mutableStateOf(TextFieldValue(uiLogic.message))
    private val recipientAddress = mutableStateOf(TextFieldValue(uiLogic.receiverAddress))
    private val editableFeeList: MutableState<List<SuggestedFee>> =
        mutableStateOf(uiLogic.suggestedFeeItems)

}

