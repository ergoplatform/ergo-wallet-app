package org.ergoplatform.desktop.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.router.navigate
import com.arkivanov.decompose.router.router
import org.ergoplatform.Application
import org.ergoplatform.desktop.settings.SettingsComponent
import org.ergoplatform.desktop.transactions.ReceiveToWalletComponent
import org.ergoplatform.desktop.transactions.SendFundsComponent
import org.ergoplatform.desktop.ui.AppBarView
import org.ergoplatform.desktop.ui.QrScannerComponent
import org.ergoplatform.desktop.wallet.*
import org.ergoplatform.mosaik.MosaikComposeDialog
import org.ergoplatform.mosaik.MosaikComposeDialogHandler
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS

/**
 * Navigator
 */
class NavHostComponent(
    componentContext: ComponentContext
) : Component, ComponentContext by componentContext {

    val router = router<ScreenConfig, Component>(
        initialConfiguration = ScreenConfig.WalletList,
        childFactory = ::createScreenComponent
    )

    val dialogHandler = MosaikComposeDialogHandler()

    /**
     * Factory function to create screen from given ScreenConfig
     */
    private fun createScreenComponent(
        screenConfig: ScreenConfig,
        componentContext: ComponentContext
    ): Component {

        return when (screenConfig) {

            is ScreenConfig.WalletList -> {
                WalletListComponent(
                    componentContext, this
                )
            }

            is ScreenConfig.AddWalletChooser ->
                AddWalletChooserComponent(componentContext, this)

            is ScreenConfig.AddReadOnlyWallet ->
                AddReadOnlyWalletComponent(componentContext, this)

            is ScreenConfig.CreateWallet ->
                CreateWalletComponent(componentContext, this)

            is ScreenConfig.ConfirmCreateWallet ->
                ConfirmCreateWalletComponent(componentContext, this, screenConfig.mnemonic)

            is ScreenConfig.RestoreWallet ->
                RestoreWalletComponent(componentContext, this)

            is ScreenConfig.SaveWallet ->
                SaveWalletComponent(
                    componentContext,
                    this,
                    screenConfig.mnemonic,
                    screenConfig.fromRestore
                )

            is ScreenConfig.WalletConfiguration ->
                WalletConfigComponent(componentContext, this, screenConfig.walletConfig)

            is ScreenConfig.SendFunds -> SendFundsComponent(
                componentContext, this,
                screenConfig.walletConfig
            )

            is ScreenConfig.ReceiveToWallet -> ReceiveToWalletComponent(
                componentContext, this, screenConfig.walletConfig
            )

            is ScreenConfig.QrCodeScanner -> QrScannerComponent(
                componentContext,
                this,
                screenConfig.callback
            )

            ScreenConfig.Settings -> SettingsComponent(
                componentContext, this
            )
        }
    }


    /**
     * Renders screen as per request
     */
    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun render(scaffoldState: ScaffoldState?) {
        val navItemState = remember { mutableStateOf(NavItem.WALLETS) }

        Box {
            val drawChildren: @Composable (ScaffoldState?) -> Unit = { scaffoldState ->
                Children(
                    routerState = router.state,
                    animation = childAnimation { child, direction ->
                        child.instance.animation(direction)
                    }
                ) { it.instance.render(scaffoldState) }
            }

            val state = router.state.subscribeAsState()
            val navClientScreenComponent =
                state.value.activeChild.instance as? NavClientScreenComponent
            val showAppBars = navClientScreenComponent?.fullScreen != true

            if (showAppBars) {
                AppBarView(
                    navClientScreenComponent?.appBarLabel ?: "",
                    navClientScreenComponent?.actions ?: {},
                    router,
                    bottombar = { BottomBar(navItemState) }
                ) { innerPadding, scaffoldState ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        drawChildren(scaffoldState)
                    }
                }
            } else {
                drawChildren(null)
            }

            MosaikComposeDialog(dialogHandler)
        }
    }

    @Composable
    private fun BottomBar(navItemState: MutableState<NavItem>) {
        BottomNavigation(backgroundColor = MaterialTheme.colors.primary) {
            BottomNavigationItem(
                icon = {
                    Icon(
                        Icons.Outlined.AccountBalanceWallet,
                        contentDescription = Application.texts.getString(
                            STRING_TITLE_WALLETS
                        )
                    )
                },
                selected = navItemState.value == NavItem.WALLETS,
                onClick = {
                    navItemState.value = NavItem.WALLETS
                    router.navigate { mutableListOf(ScreenConfig.WalletList) }
                },
                label = { Text(text = Application.texts.getString(STRING_TITLE_WALLETS)) },
            )
            BottomNavigationItem(
                icon = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = Application.texts.getString(
                            STRING_TITLE_SETTINGS
                        )
                    )
                },
                selected = navItemState.value == NavItem.SETTINGS,
                onClick = {
                    navItemState.value = NavItem.SETTINGS
                    router.navigate { listOf(ScreenConfig.Settings) }
                },
                label = { Text(text = Application.texts.getString(STRING_TITLE_SETTINGS)) },
            )
        }
    }

    private enum class NavItem {
        WALLETS, SETTINGS
    }
}

