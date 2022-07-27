package org.ergoplatform.desktop.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class NavClientScreenComponent(
    private val navHost: NavHostComponent
) : Component {

    abstract val appBarLabel: String

    open val actions: @Composable RowScope.() -> Unit = {}

    open val fullScreen = false

    val router by lazy { navHost.router }

    private var _componentScope: CoroutineScope? = null

    fun LifecycleOwner.componentScope(): CoroutineScope {
        if (_componentScope == null) {
            _componentScope = CoroutineScope(Dispatchers.Default)
            lifecycle.subscribe(onDestroy = {
                _componentScope?.cancel()
            })
        }

        return _componentScope!!
    }

    @Composable
    override fun render(scaffoldState: ScaffoldState?) {
        renderScreenContents(scaffoldState)
    }

    @Composable
    abstract fun renderScreenContents(scaffoldState: ScaffoldState?)

    open fun onNavigateBack(): Boolean = false
}