package org.ergoplatform.desktop.settings

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.ChildAnimator
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.fade
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS

class SettingsComponent(private val componentContext: ComponentContext,
                        navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_SETTINGS)

    @OptIn(ExperimentalDecomposeApi::class)
    override val animation: ChildAnimator
        get() = fade()

    @Composable
    override fun renderScreenContents() {
        SettingsScreen()
    }
}