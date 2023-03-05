package org.ergoplatform.desktop.multisig

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent

class CreateMultisigAddressComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = "" // no app bar shown

    override val fullScreen: Boolean
        get() = true

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        // TODO
    }
}