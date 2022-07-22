package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.ChildAnimator
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.Direction
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import com.arkivanov.decompose.router.push
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.MainAppUiLogic
import org.ergoplatform.uilogic.STRING_LABEL_LAST_SYNC
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS
import org.ergoplatform.uilogic.STRING_ZXING_BUTTON_OK
import org.ergoplatform.utils.getTimeSpanString

class WalletListComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_WALLETS)

    @OptIn(ExperimentalDecomposeApi::class)
    override fun animation(direction: Direction): ChildAnimator =
        when (direction) {
            Direction.ENTER_BACK -> slide()
            else -> fade()
        }

    override val actions: @Composable RowScope.() -> Unit
        get() {
            val syncManager = WalletStateSyncManager.getInstance()
            return {
                val refreshState = syncManager.isRefreshing.collectAsState(false)
                val lastRefreshMs = syncManager.lastRefreshMs
                val refreshClick: () -> Unit = {
                    syncManager.refreshByUser(Application.prefs, Application.database)
                }
                if (!refreshState.value && lastRefreshMs > 0) {
                    Text(
                        Application.texts.getString(
                            STRING_LABEL_LAST_SYNC,
                            getTimeSpanString(
                                (System.currentTimeMillis() - lastRefreshMs) / 1000L,
                                Application.texts
                            )
                        ),
                        style = labelStyle(LabelStyle.BODY2),
                        modifier = Modifier.clickable { refreshClick() }
                    )
                }

                IconButton(refreshClick) {
                    Icon(Icons.Default.Refresh, null)
                }

                IconButton({ router.push(ScreenConfig.AddWalletChooser) }) {
                    Icon(Icons.Default.Add, null)
                }

                IconButton({
                    router.push(ScreenConfig.QrCodeScanner { qrCode ->
                        onQrCodeScanned(qrCode)
                    })
                }) {
                    Icon(Icons.Default.QrCodeScanner, null)
                }

            }
        }

    init {
        lifecycle.doOnResume {
            componentScope().launch {
                delay(300) // we wait a little before doing the refresh to give DB some time
                WalletStateSyncManager.getInstance()
                    .refreshWhenNeeded(Application.prefs, Application.database)
            }
        }

        lifecycle.doOnCreate {
            componentScope().launch {
                WalletStateSyncManager.getInstance().isRefreshing.collect {
                    walletStates.value =
                        Application.database.walletDbProvider.getWalletsWithStates()
                }
            }
            componentScope().launch(Dispatchers.IO) {
                Application.database.walletDbProvider.getWalletsWithStatesFlow()
                    .collect { walletStates.value = it }
            }
        }
    }

    private val walletStates = mutableStateOf<List<Wallet>>(emptyList())
    private val chooseWalletDialogState = mutableStateOf<String?>(null)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val fiatState = WalletStateSyncManager.getInstance().fiatValue.collectAsState()
        val refreshState = WalletStateSyncManager.getInstance().isRefreshing.collectAsState(false)

        WalletListScreen(
            walletStates.value, fiatState.value, refreshState.value,
            onPushScreen = { router.push(it) },
            onSendClicked = { wallet -> navigateToSendScreen(wallet) },
            onReceiveClicked = { router.push(ScreenConfig.ReceiveToWallet(it)) },
            onSettingsClicked = { router.push(ScreenConfig.WalletConfiguration(it)) },
        )

        chooseWalletDialogState.value?.let { paymentRequestString ->
            val paymentRequest = remember { parsePaymentRequest(paymentRequestString) }
            ChooseWalletListDialog(
                walletStates.value, false,
                onWalletChosen = { walletConfig ->
                    navigateToSendScreen(walletConfig, paymentRequestString)
                    chooseWalletDialogState.value = null
                },
                onDismiss = { chooseWalletDialogState.value = null },
                header = paymentRequest?.let { { PaymentRequestHeader(paymentRequest) } }
            )
        }
    }

    private fun navigateToSendScreen(
        walletConfig: WalletConfig,
        paymentRequestString: String? = null
    ) {
        router.push(ScreenConfig.SendFunds(walletConfig, paymentRequestString))
    }

    private fun onQrCodeScanned(qrCode: String) {
        MainAppUiLogic.handleRequests(
            qrCode, true,
            Application.texts,
            navigateToChooseWalletDialog = { paymentRequest ->
                if (walletStates.value.size == 1) {
                    // go directly to send screen
                    navigateToSendScreen(walletStates.value.first().walletConfig, paymentRequest)
                } else {
                    chooseWalletDialogState.value = paymentRequest
                }
            },
            navigateToErgoPay = {
                // TODO ergopay
            },
            navigateToAuthentication = {
                // TODO ErgoAUth
            },
            presentUserMessage = { message ->
                navHost.dialogHandler.showDialog(
                    MosaikDialog(
                        message,
                        Application.texts.getString(STRING_ZXING_BUTTON_OK),
                        null, null, null
                    )
                )
            }
        )
    }
}



