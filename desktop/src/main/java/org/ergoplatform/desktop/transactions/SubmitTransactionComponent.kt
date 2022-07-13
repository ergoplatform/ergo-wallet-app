package org.ergoplatform.desktop.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.desktop.ui.PasswordDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.proceedAuthFlowWithPassword
import org.ergoplatform.uilogic.transactions.SubmitTransactionUiLogic

abstract class SubmitTransactionComponent(
    val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    protected abstract val uiLogic: SubmitTransactionUiLogic

    protected val passwordDialog = mutableStateOf(false)

    @Composable
    protected fun SubmitTransactionOverlays() {
        if (passwordDialog.value) {
            PasswordDialog(
                onDismissRequest = { passwordDialog.value = false },
                onPasswordEntered = {
                    proceedAuthFlowWithPassword(
                        it,
                        uiLogic.wallet!!.walletConfig,
                        ::proceedFromAuthFlow
                    )
                }
            )
        }
    }

    protected fun startPayment() {
        val walletConfig = uiLogic.wallet!!.walletConfig
        walletConfig.secretStorage?.let {
            passwordDialog.value = true
        } ?: uiLogic.startColdWalletPayment(Application.prefs, Application.texts)
    }

    private fun proceedFromAuthFlow(signingSecrets: SigningSecrets) {
        uiLogic.startPaymentWithMnemonicAsync(
            signingSecrets,
            Application.prefs,
            Application.texts,
            Application.database
        )
    }
}