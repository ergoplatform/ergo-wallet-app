package org.ergoplatform.desktop.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig

class WalletListComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    private var state by mutableStateOf(Model())

    override val appBarLabel: String
        get() = "Input"

    @Composable
    override fun renderScreenContents() {
        InputScreen(
            name = state.name,
            onGoClicked = { router.push(ScreenConfig.SendFunds(state.name)) },
            onTextChanged = {
                state = state.copy(name = it)
            }
        )
    }

    private data class Model(
        val name: String = ""
    )
}



