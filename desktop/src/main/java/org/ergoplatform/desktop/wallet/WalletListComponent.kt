package org.ergoplatform.desktop.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.Dispatchers
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS

class WalletListComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_WALLETS)

    @Composable
    override fun renderScreenContents() {
        val state = Application.database.walletDbProvider.getWalletsWithStatesFlow()
            .collectAsState(
                emptyList(), Dispatchers.IO
            )

        WalletListScreen(
            state.value,
            onSendClicked = { router.push(ScreenConfig.SendFunds(it)) },
            onReceiveClicked = { router.push(ScreenConfig.ReceiveToWallet(it)) },
        )
    }
}



