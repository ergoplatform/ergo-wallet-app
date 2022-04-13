package org.ergoplatform.desktop.transactions

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.essenty.lifecycle.Lifecycle
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.uilogic.STRING_BUTTON_SEND

class SendFundsComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val name: String,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    companion object {
        private val greetings = listOf(
            "Bonjour",
            "Hola",
            "Olá",
            "Ciao",
            "Hi",
            "Hallo",
            "Hey"
        )
    }

    private val greeting = greetings.random()

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onCreate() {
                println("onCreate")
            }

            override fun onDestroy() {
                println("onDestroy")
            }

            override fun onPause() {
                println("onPause")
            }

            override fun onResume() {
                println("onRsume")
            }

            override fun onStart() {
                println("onStart")
            }

            override fun onStop() {
                println("onStop")
            }

        })
    }

    override val appBarLabel: String
        get() = Application.texts.get(STRING_BUTTON_SEND)

    @Composable
    override fun renderScreenContents() {
        SendFundsScreen(
            greeting = "$greeting, $name",
            onGoBackClicked = { navHost.router.pop() }
        )
    }
}

