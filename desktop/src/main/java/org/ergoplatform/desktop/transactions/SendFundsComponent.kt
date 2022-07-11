package org.ergoplatform.desktop.transactions

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic

class SendFundsComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val waletConfig: WalletConfig,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_BUTTON_SEND)

    private val walletAddressState = mutableStateOf<WalletAddress?>(null)
    private val recipientAddress = mutableStateOf(TextFieldValue())
    private val recipientError = mutableStateOf(false)
    private val amountToSend = mutableStateOf(TextFieldValue())
    private val amountError = mutableStateOf(false)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        SendFundsScreen(
            waletConfig,
            walletAddressState.value,
            recipientAddress,
            amountToSend,
            recipientError.value,
            amountError.value,
            uiLogic.feeAmount,
            uiLogic.grossAmount,
            onChooseToken = {

            },
            onMaxAmountClicked = {
                uiLogic.setAmountToSendErg(uiLogic.getMaxPossibleAmountToSend())
            }
        )
    }

    val uiLogic = object : SendFundsUiLogic() {
        override fun notifyTokensChosenChanged() {
            TODO("Not yet implemented")
        }

        override fun notifyAmountsChanged() {
            TODO("Not yet implemented")
        }

        override fun notifyBalanceChanged() {
            TODO("Not yet implemented")
        }

        override fun showErrorMessage(message: String) {
            TODO("Not yet implemented")
        }

        override fun onNotifySuggestedFees() {
            TODO("Not yet implemented")
        }

        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyWalletStateLoaded() {
            TODO("Not yet implemented")
        }

        override fun notifyDerivedAddressChanged() {
            walletAddressState.value = derivedAddress
        }

        override fun notifyUiLocked(locked: Boolean) {
            TODO("Not yet implemented")
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

    }
}

