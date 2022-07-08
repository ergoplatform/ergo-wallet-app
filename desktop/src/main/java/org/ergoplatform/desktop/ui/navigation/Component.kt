package org.ergoplatform.desktop.ui.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.Direction
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide

interface Component {
    @Composable
    fun render(scaffoldState: ScaffoldState?)

    @OptIn(ExperimentalDecomposeApi::class)
    fun animation(direction: Direction) = slide()
}