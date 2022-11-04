package org.ergoplatform.desktop.ergoauth

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.desktop.transactions.SigningPromptDialog
import org.ergoplatform.desktop.ui.PasswordDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.ui.proceedAuthFlowWithPassword
import org.ergoplatform.desktop.wallet.ChooseWalletListDialog
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.STRING_TITLE_ERGO_AUTH_REQUEST
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic
import org.ergoplatform.wallet.isReadOnly

class ErgoAuthComponent(
    private val request: String,
    private val walletId: Int?,
    private val doOnComplete: (() -> Unit)? = null,
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_ERGO_AUTH_REQUEST)

    private val uiLogic = DesktopErgoAuthUiLogic().apply {
        init(request, walletId ?: -1, Application.texts, Application.database)
    }

    private val authState = mutableStateOf(ErgoAuthUiLogic.State.FETCHING_DATA)
    private val chooseWalletDialog = mutableStateOf<List<Wallet>?>(null)
    private val passwordDialog = mutableStateOf(false)

    // state on the hot device
    private val signingPromptState = mutableStateOf<String?>(null)
    private val signingPromptPagesScanned = mutableStateOf<Pair<Int, Int>?>(null)

    // state on the cold device
    private val scanningState = mutableStateOf(0)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        ErgoAuthScreen(
            uiLogic,
            authState.value,
            onChangeWallet = {
                componentScope().launch {
                    chooseWalletDialog.value =
                        Application.database.walletDbProvider.getWalletsWithStates()
                }
            },
            onAuthenticate = {
                if (uiLogic.walletConfig?.isReadOnly() == false)
                    passwordDialog.value = true
                else
                    signingPromptState.value = uiLogic.ergAuthRequest?.toColdAuthRequest()
            },
            onDismiss = router::pop,
            scanningState = scanningState,
            onScan = {
                router.push(ScreenConfig.QrCodeScanner { qrCodeChunk ->
                    uiLogic.addRequestQrPage(qrCodeChunk, Application.texts)
                    scanningState.value = scanningState.value + 1
                })
            }
        )

        if (chooseWalletDialog.value != null) {
            val walletList = chooseWalletDialog.value!!
            ChooseWalletListDialog(
                walletList, true,
                onWalletChosen = { walletConfig ->
                    chooseWalletDialog.value = null
                    uiLogic.walletConfig = walletConfig
                },
                onDismiss = { chooseWalletDialog.value = null },
            )
        }

        if (passwordDialog.value) {
            PasswordDialog(
                onDismissRequest = { passwordDialog.value = false },
                onPasswordEntered = {
                    proceedAuthFlowWithPassword(
                        it,
                        uiLogic.walletConfig!!,
                        uiLogic::startResponse
                    )
                }
            )
        }

        if (signingPromptState.value != null) {
            SigningPromptDialog(
                uiLogic.signingPromptDialogConfig,
                onContinueClicked = {
                    router.push(ScreenConfig.QrCodeScanner { qrCode ->
                        println(qrCode)
                        uiLogic.signingPromptDialogConfig.responsePagesCollector.let {
                            it.addPage(qrCode)
                            if (it.hasAllPages()) {
                                signingPromptState.value = null
                                uiLogic.startResponseFromCold(Application.texts)
                            } else {
                                signingPromptPagesScanned.value = Pair(it.pagesAdded, it.pagesCount)
                            }
                        }
                    })
                },
                pagesScanned = signingPromptPagesScanned.value?.first,
                pagesToScan = signingPromptPagesScanned.value?.second,
                onDismissRequest = { signingPromptState.value = null },
            )
        }
    }

    inner class DesktopErgoAuthUiLogic : ErgoAuthUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyStateChanged(newState: State) {
            authState.value = newState

            if (newState == State.DONE && getDoneSeverity() != MessageSeverity.ERROR)
                doOnComplete?.invoke()
        }

        fun startResponse(signingSecrets: SigningSecrets) {
            startResponse(signingSecrets, Application.texts)
        }
    }
}