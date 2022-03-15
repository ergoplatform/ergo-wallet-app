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
import org.ergoplatform.Application
import org.ergoplatform.desktop.transactions.SendFundsComponent
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
                                contentDescription = Application.texts.get(STRING_TITLE_WALLETS)
                            )
                        },
                        selected = navItemState.value == NavItem.WALLETS,
                        onClick = {
                            navItemState.value = NavItem.WALLETS
                            router.navigate { mutableListOf(ScreenConfig.WalletList) }
                        },
                        label = { Text(text = Application.texts.get(STRING_TITLE_WALLETS)) },
                    )
                    BottomNavigationItem(
                        icon = {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = Application.texts.get(STRING_TITLE_SETTINGS)
                            )
                        },
                        selected = navItemState.value == NavItem.SETTINGS,
                        onClick = {
                            navItemState.value = NavItem.SETTINGS
                            router.navigate { listOf(ScreenConfig.WalletList) }
                        },
                        label = { Text(text = Application.texts.get(STRING_TITLE_SETTINGS)) },
                    )
                }
            }
        }
    }

    private enum class NavItem {
        WALLETS, SETTINGS
    }
}

