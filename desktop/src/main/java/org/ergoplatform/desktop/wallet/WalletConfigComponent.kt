package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.ConfirmationDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_BUTTON_DELETE
import org.ergoplatform.uilogic.STRING_LABEL_CHANGES_SAVED
import org.ergoplatform.uilogic.STRING_LABEL_CONFIRM_DELETE
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_DETAILS
import org.ergoplatform.uilogic.wallet.WalletConfigUiLogic

class WalletConfigComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
    walletConfig: WalletConfig,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_WALLET_DETAILS)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton({
                confirmationDialogState.value = true
            }) {
                Icon(
                    Icons.Default.Delete,
                    null,
                )
            }
        }

    private fun doDeleteWallet() {
        // GlobalScope to let deletion process when fragment is already dismissed
        GlobalScope.launch {
            val walletDbProvider = Application.database.walletDbProvider
            walletDbProvider.withTransaction {
                val walletConfig = walletConfigState.value
                walletConfig.firstAddress?.let { firstAddress ->
                    walletDbProvider.deleteWalletConfigAndStates(
                        firstAddress,
                        walletConfig.id
                    )
                }
            }
        }
        router.pop()
    }

    val walletConfigState = mutableStateOf(walletConfig)
    val confirmationDialogState = mutableStateOf(false)

    val uiLogic = object : WalletConfigUiLogic() {
        init {
            wallet = walletConfig
        }

        override fun onConfigChanged(value: WalletConfig?) {
            value?.let { walletConfigState.value = value }
        }
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        WalletConfigScreen(walletConfigState.value, scaffoldState,
            onChangeName = { newName ->
                val saved = uiLogic.saveChanges(Application.database.walletDbProvider, newName)
                if (saved) {
                    scaffoldState?.snackbarHostState?.showSnackbar(
                        Application.texts.getString(
                            STRING_LABEL_CHANGES_SAVED
                        )
                    )
                }
            })

        if (confirmationDialogState.value) {
            ConfirmationDialog(Application.texts.getString(STRING_BUTTON_DELETE),
                Application.texts.getString(STRING_LABEL_CONFIRM_DELETE),
                onDismissRequest = {
                    confirmationDialogState.value = false
                },
                onConfirmation = {
                    doDeleteWallet()
                },
            )
        }
    }
}