package org.ergoplatform.desktop.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import com.arkivanov.decompose.router.router
import org.ergoplatform.desktop.transactions.SendFundsComponent
import org.ergoplatform.desktop.wallet.WalletListComponent

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

            is ScreenConfig.SendFunds -> SendFundsComponent(
                componentContext, this,
                screenConfig.name
            )
            ScreenConfig.Settings -> TODO()
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
            Children(
                modifier = Modifier.weight(1f, true),
                routerState = router.state,
                animation = slide()
            ) { it.instance.render() }

            CompositionLocalProvider(LocalElevationOverlay provides null) {
                BottomNavigation(backgroundColor = MaterialTheme.colors.primary) {
                    BottomNavigationItem(
                        icon = {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "Wallets"
                            ) // TODO Desktop i18n
                        },
                        selected = navItemState.value == NavItem.WALLETS,
                        onClick = {
                            navItemState.value = NavItem.WALLETS
                            router.navigate { mutableListOf(ScreenConfig.WalletList) }
                        },
                        label = { Text(text = "Wallets") },
                    )
                    BottomNavigationItem(
                        icon = {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "Settings" // TODO Desktop i18n
                            )
                        },
                        selected = navItemState.value == NavItem.SETTINGS,
                        onClick = {
                            navItemState.value = NavItem.SETTINGS
                            router.navigate { listOf(ScreenConfig.WalletList) }
                        },
                        label = { Text(text = "Settings") },
                    )
                }
            }
        }
    }

    private enum class NavItem {
        WALLETS, SETTINGS
    }
}

