package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
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
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_LABEL_LAST_SYNC
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS
import org.ergoplatform.utils.getTimeSpanString

class WalletListComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_WALLETS)

    override val actions: @Composable RowScope.() -> Unit
        get() {
            val syncManager = WalletStateSyncManager.getInstance()
            return {
                val refreshState = syncManager.isRefreshing.collectAsState(false)
                val lastRefresMs = syncManager.lastRefreshMs
                val refreshClick: () -> Unit = {
                    syncManager.refreshByUser(Application.prefs, Application.database)
                }
                if (!refreshState.value && lastRefresMs > 0) {
                    Text(
                        Application.texts.getString(
                            STRING_LABEL_LAST_SYNC,
                            getTimeSpanString(
                                (System.currentTimeMillis() - lastRefresMs) / 1000L,
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

    @Composable
    override fun renderScreenContents() {
        val fiatState = WalletStateSyncManager.getInstance().fiatValue.collectAsState()
        val refreshState = WalletStateSyncManager.getInstance().isRefreshing.collectAsState(false)

        WalletListScreen(
            walletStates.value, fiatState.value, refreshState.value,
            onSendClicked = { router.push(ScreenConfig.SendFunds(it)) },
            onReceiveClicked = { router.push(ScreenConfig.ReceiveToWallet(it)) },
        )
    }
}



