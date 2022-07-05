package org.ergoplatform.desktop.ui.navigation

import androidx.compose.foundation.layout.Column
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
import org.ergoplatform.desktop.wallet.AddReadOnlyWalletComponent
import org.ergoplatform.desktop.wallet.WalletListComponent
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
                // TODO chooser, not read only
                AddReadOnlyWalletComponent(componentContext, this)

            is ScreenConfig.SendFunds -> SendFundsComponent(
                componentContext, this,
                screenConfig.name
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
    override fun render() {
        val navItemState = remember { mutableStateOf(NavItem.WALLETS) }

        Column {
            val drawChildren = @Composable {
                Children(
                    modifier = Modifier.weight(1f, true),
                    routerState = router.state,
                    animation = childAnimation { child, direction ->
                        child.instance.animation(direction)
                    }
                ) { it.instance.render() }
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
                ) { drawChildren() }
            } else {
                drawChildren()
            }
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

