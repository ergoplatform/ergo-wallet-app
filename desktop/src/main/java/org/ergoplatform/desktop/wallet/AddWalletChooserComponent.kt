package org.ergoplatform.desktop.wallet

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent

class AddWalletChooserComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = "" // no app bar shown

    override val fullScreen: Boolean
        get() = true

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        AddWalletChooserScreen(
            { config ->
                router.push(config)
            },
            onDismiss = {
                router.pop()
            },
        )
    }

}