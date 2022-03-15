package org.ergoplatform

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.ResourceWrapper
import org.ergoplatform.desktop.ui.DecomposeDesktopExampleTheme
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.uilogic.STRING_APP_NAME

@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    val root = NavHostComponent(DefaultComponentContext(lifecycle = lifecycle))

    application {
        val windowState = rememberWindowState()
        LifecycleController(lifecycle, windowState)

        Application.texts = I18NBundle.createBundle(ResourceWrapper("/i18n/strings"))

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = Application.texts.get(STRING_APP_NAME)
        ) {
            DecomposeDesktopExampleTheme {
                root.render()
            }
        }
    }

}

object Application {
    lateinit var texts: I18NBundle
}