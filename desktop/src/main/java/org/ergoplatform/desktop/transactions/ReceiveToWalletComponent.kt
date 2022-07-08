package org.ergoplatform.desktop.transactions

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.runBlocking
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic

class ReceiveToWalletComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
    private val walletConfig: WalletConfig,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_BUTTON_RECEIVE)

    private val uiLogic = ReceiveToWalletUiLogic()

    init {
        runBlocking { // TODO check if we can do better
            uiLogic.loadWallet(walletConfig.id, Application.database.walletDbProvider)
        }
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        ReceiveToWalletScreen(walletConfig, uiLogic.address!!, scaffoldState)
    }
}