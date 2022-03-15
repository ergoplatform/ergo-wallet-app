package com.theapache64.dde.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.router
import com.arkivanov.essenty.parcelable.Parcelable

/**
 * Navigator
 */
class NavHostComponent(
    componentContext: ComponentContext
) : Component, ComponentContext by componentContext {

    private val router = router<ScreenConfig, Component>(
        initialConfiguration = ScreenConfig.Input,
        childFactory = ::createScreenComponent
    )

    /**
     * Factory function to create screen from given ScreenConfig
     */
    private fun createScreenComponent(
        screenConfig: ScreenConfig,
        componentContext: ComponentContext
    ): Component {
        TODO("Desktop add navigation")
//        return when (screenConfig) {
//
//            is ScreenConfig.Input -> {
//                InputScreenComponent(
//                    componentContext,
//                    {}
//                )
//            }
//
//            is ScreenConfig.Greeting -> GreetingScreenComponent(
//                componentContext,
//                screenConfig.name,
//                {}
//            )
//        }
    }


    /**
     * Renders screen as per request
     */
    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun render() {
        Children(
            routerState = router.state,
            animation = slide()
        ) {
            Column {
                TopAppBar(
                    title = { Text("Title bar") }, // TODO desktop, and add to BaseScreen
                    navigationIcon = if (router.state.value.backStack.isEmpty()) null else ({
                        IconButton(onClick = router::pop) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null
                            )
                        }
                    })
                )
                Box(modifier = Modifier.weight(1f, true)) {
                    it.instance.render()
                }
            }
        }
    }

    private sealed class ScreenConfig : Parcelable {
        object Input : ScreenConfig()
        data class Greeting(val name: String) : ScreenConfig()
    }
}

