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
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.getSerializedXpubKeyFromMnemonic
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.WalletConfigUiLogic

class WalletConfigComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
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

    private val walletConfigState = mutableStateOf(walletConfig)
    private val confirmationDialogState = mutableStateOf(false)
    private val passwordDialog = mutableStateOf(PasswordNeededFor.Nothing)
    private val shareWithQrDialogState = mutableStateOf<String?>(null)

    val uiLogic = object : WalletConfigUiLogic() {
        init {
            wallet = walletConfig
            componentScope().launch { loadMultisigInfo() }
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
            },
            onAddAddresses = {
                router.push(ScreenConfig.WalletAddressesList(walletConfigState.value))
            },
            onShowXpubKey = {
                uiLogic.wallet?.secretStorage?.let {
                    passwordDialog.value = PasswordNeededFor.SHOW_XPUB
                } ?: uiLogic.wallet?.extendedPublicKey?.let {
                    displayXpubKey(it)
                }
            },
            onShowMnemonic = {
                passwordDialog.value = PasswordNeededFor.DISPLAY_MNEMONIC
            },
            multiSigStateFlow = uiLogic.multisigInfoFlow
        )

        if (confirmationDialogState.value) {
            ConfirmationDialog(
                Application.texts.getString(STRING_BUTTON_DELETE),
                Application.texts.getString(STRING_LABEL_CONFIRM_DELETE),
                onDismissRequest = {
                    confirmationDialogState.value = false
                },
                onConfirmation = {
                    doDeleteWallet()
                },
            )
        }

        if (passwordDialog.value != PasswordNeededFor.Nothing) {
            PasswordDialog(
                onDismissRequest = { passwordDialog.value = PasswordNeededFor.Nothing },
                onPasswordEntered = {
                    proceedAuthFlowWithPassword(it, uiLogic.wallet!!, ::proceedFromAuthFlow)
                }
            )
        }

        shareWithQrDialogState.value?.let { qrMsg ->
            ShareWithQrDialog(qrMsg, onDismiss = { shareWithQrDialogState.value = null })
        }
    }

    private fun proceedFromAuthFlow(signingSecrets: SigningSecrets) {
        when (passwordDialog.value) {
            PasswordNeededFor.Nothing -> {}
            PasswordNeededFor.DISPLAY_MNEMONIC -> showMnemonic(signingSecrets)
            PasswordNeededFor.SHOW_XPUB -> displayXpubKeyFromMnemonic(signingSecrets)
        }
    }

    private fun displayXpubKeyFromMnemonic(signingSecrets: SigningSecrets) {
        val xpubkey = getSerializedXpubKeyFromMnemonic(signingSecrets)
        signingSecrets.clearMemory()
        displayXpubKey(xpubkey)
    }

    private fun displayXpubKey(xpubkey: String) {
        shareWithQrDialogState.value = xpubkey
    }

    private fun showMnemonic(signingSecrets: SigningSecrets) {
        val mnemonic = signingSecrets.mnemonic.toStringUnsecure()
        signingSecrets.clearMemory()

        navHost.dialogHandler.showDialog(
            MosaikDialog(
                mnemonic,
                Application.texts.getString(STRING_BUTTON_COPY),
                Application.texts.getString(STRING_LABEL_DISMISS),
                {
                    showSensitiveDataCopyDialog(navHost, mnemonic)
                },
                null
            )
        )
    }

    enum class PasswordNeededFor { Nothing, DISPLAY_MNEMONIC, SHOW_XPUB }
}