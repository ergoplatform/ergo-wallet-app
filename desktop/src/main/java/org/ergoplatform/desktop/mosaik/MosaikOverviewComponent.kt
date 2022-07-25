package org.ergoplatform.desktop.mosaik

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.ChildAnimator
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.Direction
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.slide
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.mosaik.MosaikAppOverviewUiLogic
import org.ergoplatform.uilogic.STRING_TITLE_MOSAIK

class MosaikOverviewComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    @OptIn(ExperimentalDecomposeApi::class)
    override fun animation(direction: Direction): ChildAnimator =
        when (direction) {
            Direction.ENTER_BACK -> slide()
            else -> fade()
        }

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_MOSAIK)

    private val uiLogic = DesktopUiLogic().apply {
        init(Application.database)
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val favoritesList = uiLogic.favoritesFlow.collectAsState()
        val lastVisitedList = uiLogic.lastVisitedFlow.collectAsState()

        MosaikOverviewScreen(
            favoritesList.value,
            lastVisitedList.value,
            onAddressEntered = ::navigateTo,
            onAppClicked = { navigateTo(it.url, title = it.name) }
        )
    }

    private fun navigateTo(address: String, title: String? = null) {
        router.push(ScreenConfig.MosaikApp(title, address))
    }

    private inner class DesktopUiLogic : MosaikAppOverviewUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()
    }
}