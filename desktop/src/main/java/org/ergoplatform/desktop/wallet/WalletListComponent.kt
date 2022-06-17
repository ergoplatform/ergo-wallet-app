package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS

class WalletListComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_WALLETS)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton({ router.push(ScreenConfig.AddWalletChooser) }) {
                Icon(Icons.Default.Add, null)
            }
        }

    init {
        lifecycle.doOnResume {
            WalletStateSyncManager.getInstance()
                .refreshWhenNeeded(Application.prefs, Application.database)
        }

        lifecycle.doOnCreate {
            componentScope().launch {
                WalletStateSyncManager.getInstance().isRefreshing.collect {
                    walletStates.value = Application.database.walletDbProvider.getWalletsWithStates()
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



