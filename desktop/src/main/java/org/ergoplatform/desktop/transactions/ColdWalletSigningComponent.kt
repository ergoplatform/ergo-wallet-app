package org.ergoplatform.desktop.transactions

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.PasswordDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.ui.proceedAuthFlowWithPassword
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.STRING_ERROR_PREPARE_TRANSACTION
import org.ergoplatform.uilogic.STRING_TITLE_SIGNING_REQUEST
import org.ergoplatform.uilogic.transactions.ColdWalletSigningUiLogic

class ColdWalletSigningComponent(
    private val signingRequestChunk: String,
    private val walletId: Int,
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_SIGNING_REQUEST)

    private val uiLogic = DesktopUiLogic().apply {
        setWalletId(walletId, Application.database.walletDbProvider)
        addQrCodeChunk(signingRequestChunk)
    }

    private val scanningState = mutableStateOf(0)
    private val txInfoState = mutableStateOf(uiLogic.transactionInfo?.reduceBoxes())
    private val passwordInputState = mutableStateOf(false)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        ColdWalletSigningScreen(
            uiLogic,
            scanningState,
            txInfoState,
            ::scanNextQr,
            ::onConfirm,
            router::pop,
        )
        if (passwordInputState.value) {
            PasswordDialog(
                onDismissRequest = { passwordInputState.value = false },
                onPasswordEntered = {
                    proceedAuthFlowWithPassword(it, uiLogic.wallet!!.walletConfig,
                        proceedFromAuthFlow = { signingSecrets ->
                            uiLogic.signTxWithMnemonicAsync(signingSecrets, Application.texts)
                        })
                }
            )
        }

    }

    private fun onConfirm() {
        passwordInputState.value = true
    }

    private fun scanNextQr() {
        router.push(ScreenConfig.QrCodeScanner { qrCodeChunk ->
            uiLogic.addQrCodeChunk(qrCodeChunk)
            scanningState.value = scanningState.value + 1
            uiLogic.transactionInfo?.let {
                txInfoState.value = it.reduceBoxes()
            }
        })
    }

    private inner class DesktopUiLogic : ColdWalletSigningUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyUiLocked(locked: Boolean) {
            navHost.lockScreen.value = locked
        }

        override fun notifySigningResult(ergoTxResult: SigningResult) {
            if (!ergoTxResult.success) {
                navHost.showErrorDialog(Application.texts.getString(STRING_ERROR_PREPARE_TRANSACTION)
                        + ergoTxResult.errorMsg?.let { "\n\n$it" })
            }
        }
    }
}