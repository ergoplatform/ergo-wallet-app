package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_TITLE_ERGO_PAY_REQUEST
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic
import org.ergoplatform.uilogic.transactions.SubmitTransactionUiLogic

class ErgoPaySigningComponent(
    private val request: String,
    private val walletId: Int?,
    private val derivationIndex: Int?,
    componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : SubmitTransactionComponent(componentContext, navHost) {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_ERGO_PAY_REQUEST)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            // TODO Reload button
        }

    private val ergoPayUiLogic = DesktopErgoPayUiLogic()

    override val uiLogic: SubmitTransactionUiLogic = ergoPayUiLogic

    private val ergoPayState = mutableStateOf(ErgoPaySigningUiLogic.State.FETCH_DATA)

    init {
        ergoPayUiLogic.init(
            request,
            walletId ?: -1,
            derivationIndex ?: -1,
            Application.database.walletDbProvider,
            Application.prefs,
            Application.texts,
        )
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        ErgoPaySigningScreen(ergoPayState.value,
            ergoPayUiLogic,
            onReload = {
                ergoPayUiLogic.reloadFromDapp(
                    Application.prefs,
                    Application.texts,
                    Application.database.walletDbProvider
                )
            },
            onDismiss = router::pop
        )
    }

    private inner class DesktopErgoPayUiLogic : ErgoPaySigningUiLogic() {
        override fun notifyStateChanged(newState: State) {
            ergoPayState.value = newState
        }

        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyWalletStateLoaded() {
            // not needed, notifyDerivedAddressChanged is always called right after this one
        }

        override fun notifyDerivedAddressChanged() {
            // TODO
        }

        override fun notifyUiLocked(locked: Boolean) {
            navHost.lockScreen.value = locked
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            if (!txResult.success) {
                navHost.showErrorDialog(getTransactionResultErrorMessage(txResult))
            }
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            TODO("Not yet implemented")
        }

    }
}