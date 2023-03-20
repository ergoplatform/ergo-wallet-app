package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressesListDialog
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.TransactionListManager
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic

class WalletDetailsComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val walletConfig: WalletConfig,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = walletConfig.displayName ?: ""

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton({
                uiLogic.refreshByUser(
                    Application.prefs, Application.database, Application.texts,
                    rescheduleRefreshJob = null
                )
            }) {
                Icon(Icons.Default.Refresh, null)
            }

            IconButton({
                router.push(ScreenConfig.WalletConfiguration(walletConfig))
            }) {
                Icon(
                    Icons.Default.Settings,
                    null,
                )
            }
        }

    private val uiLogic = DesktopWalletDetailsUiLogic().apply {
        setUpWalletStateFlowCollector(Application.database, walletConfig.id)
    }

    private var collectConfigChanges: Job? = null

    init {
        lifecycle.doOnResume {
            startRefreshWhenNeeded()
            collectConfigChanges = componentScope().launch {
                // walletWithStateByIdAsFlow does not observe state and tokens, so we need observe
                // WalletStateSyncManager here too
                WalletStateSyncManager.getInstance().isRefreshing.collect { isRefreshing ->
                    if (!isRefreshing && uiLogic.wallet != null) {
                        uiLogic.onWalletStateChanged(
                            Application.database.walletDbProvider.loadWalletWithStateById(
                                walletConfig.id
                            ),
                            Application.database.tokenDbProvider
                        )
                    }
                }
            }
        }
        lifecycle.doOnPause {
            collectConfigChanges?.cancel()
        }
    }

    private fun startRefreshWhenNeeded() {
        uiLogic.refreshWhenNeeded(
            Application.prefs,
            Application.database,
            Application.texts,
            rescheduleRefreshJob = null
        )
    }

    private val chooseAddressDialog = mutableStateOf(false)
    private val informationVersionState = mutableStateOf(0)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val syncingState = WalletStateSyncManager.getInstance().isRefreshing.collectAsState()
        val downloadingTransactionsState = TransactionListManager.isDownloading.collectAsState()

        uiLogic.wallet?.let {
            WalletDetailsScreen(
                uiLogic,
                informationVersionState.value,
                downloadingTransactionsState.value,
                onChooseAddressClicked = { chooseAddressDialog.value = true },
                onScanClicked = {
                    router.push(ScreenConfig.QrCodeScanner { qrCode -> handleQrCode(qrCode) })
                },
                onReceiveClicked = {
                    router.push(
                        ScreenConfig.ReceiveToWallet(
                            walletConfig,
                            uiLogic.addressIdx ?: 0
                        )
                    )
                },
                onSendClicked = {
                    router.push(
                        ScreenConfig.SendFunds(walletConfig, derivationIndex = uiLogic.addressIdx)
                    )
                },
                onAddressesClicked = { router.push(ScreenConfig.WalletAddressesList(walletConfig)) },
                onTokenClicked = { tokenId, balance ->
                    router.push(ScreenConfig.TokenInformation(tokenId, balance))
                },
                onTransactionClicked = { txId, address ->
                    router.push(ScreenConfig.TransactionInfo(txId, address))
                },
                onViewTransactionsClicked = {
                    router.push(
                        ScreenConfig.AddressTransactions(walletConfig, uiLogic.addressIdx ?: 0)
                    )
                },
                onMultisigTransactionClicked = {
                    // TODO 167
                }
            )
        }

        if (syncingState.value || downloadingTransactionsState.value) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (chooseAddressDialog.value) {
            ChooseAddressesListDialog(
                uiLogic.wallet!!,
                true,
                onAddressChosen = { walletAddress ->
                    chooseAddressDialog.value = false
                    uiLogic.newAddressIdxChosen(
                        walletAddress?.derivationIndex,
                        Application.prefs,
                        Application.database
                    )
                },
                onDismiss = { chooseAddressDialog.value = false },
            )
        }
    }

    private fun handleQrCode(qrCode: String) {
        uiLogic.qrCodeScanned(
            qrCode,
            Application.texts,
            navigateToColdWalletSigning = { data ->
                router.push(ScreenConfig.ColdSigning(walletConfig.id, data))
            },
            navigateToErgoPaySigning = { ergoPayRequest ->
                router.push(
                    ScreenConfig.ErgoPay(
                        ergoPayRequest,
                        walletConfig.id,
                        uiLogic.addressIdx
                    )
                )
            },
            navigateToSendFundsScreen = { paymentRequest ->
                router.push(
                    ScreenConfig.SendFunds(
                        walletConfig,
                        paymentRequest,
                        uiLogic.addressIdx
                    )
                )
            },
            navigateToAuthentication = { authRequest ->
                router.push(ScreenConfig.ErgoAuth(authRequest, walletConfig.id))
            },
            showErrorMessage = { message -> navHost.showErrorDialog(message) }
        )
    }

    private inner class DesktopWalletDetailsUiLogic : WalletDetailsUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun onDataChanged() {
            if (uiLogic.wallet == null) {
                // wallet was deleted from config screen
                router.pop()
                return
            }

            informationVersionState.value = informationVersionState.value + 1

            uiLogic.gatherTokenInformation(
                Application.database.tokenDbProvider,
                ApiServiceManager.getOrInit(Application.prefs)
            )

            startRefreshWhenNeeded()
        }

        override fun onNewTokenInfoGathered(tokenInformation: TokenInformation) {
            informationVersionState.value = informationVersionState.value + 1
        }
    }
}