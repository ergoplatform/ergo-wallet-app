package org.ergoplatform.desktop.ui.navigation

import androidx.compose.runtime.Composable
import org.ergoplatform.desktop.ui.AppBarView

abstract class NavClientScreenComponent(
    private val navHost: NavHostComponent
) : Component {

    abstract val appBarLabel: String

    val router by lazy { navHost.router }

    @Composable
    override fun render() {
        AppBarView(appBarLabel, router) { renderScreenContents() }
    }

    @Composable
    abstract fun renderScreenContents()
}