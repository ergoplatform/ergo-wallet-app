package org.ergoplatform.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.ChildAnimator
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.Direction
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS
import org.ergoplatform.uilogic.settings.SettingsUiLogic

class SettingsComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_SETTINGS)

    @OptIn(ExperimentalDecomposeApi::class)
    override fun animation(direction: Direction): ChildAnimator =
        when (direction) {
            Direction.ENTER_BACK -> slide()
            else -> fade()
        }

    private val uiLogic = SettingsUiLogic()

    @Composable
    override fun renderScreenContents() {
        val currencyButtonTextState = remember {
            mutableStateOf(
                uiLogic.getFiatCurrencyButtonText(
                    Application.prefs,
                    Application.texts
                )
            )
        }

        SettingsScreen(
            currencyButtonTextState,
            onChangeCurrencyClicked = {}
        )
    }
}