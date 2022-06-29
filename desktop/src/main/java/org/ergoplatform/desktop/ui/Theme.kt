package org.ergoplatform.desktop.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.pop
import org.ergoplatform.desktop.ui.navigation.Component
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.mosaik.MosaikStyleConfig

val uiErgoColor = MosaikStyleConfig.primaryLabelColor
val secondary = Color(24, 25, 29)
val defaultPadding = 16.dp

private val DarkColors = darkColors(
    primary = uiErgoColor,
    secondary = uiErgoColor,
    surface = secondary,
    onPrimary = Color.White,
    onSecondary = Color.White
)

@Composable
fun primaryButtonColors() = buttonColors(
    MosaikStyleConfig.primaryLabelColor,
    MosaikStyleConfig.primaryButtonTextColor
)

@Composable
fun secondaryButtonColors() = buttonColors(
    MosaikStyleConfig.secondaryButtonColor,
    MosaikStyleConfig.secondaryButtonTextColor
)

@Composable
fun DecomposeDesktopExampleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = DarkColors,
        typography = Typography(
            defaultFontFamily = FontFamily(Font("google_sans_regular.ttf"))
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun WalletAppBar(
    title: String,
    router: Router<ScreenConfig, Component>,
    actions: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalElevationOverlay provides null) {
        TopAppBar(
            backgroundColor = MaterialTheme.colors.primary,
            title = { Text(title) },
            actions = actions,
            navigationIcon = if (router.state.value.backStack.isEmpty()) null else ({
                IconButton(onClick = router::pop) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            })
        )
    }
}

@Composable
fun AppBarView(
    title: String,
    actions: @Composable RowScope.() -> Unit,
    router: Router<ScreenConfig, Component>,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(topBar = { WalletAppBar(title, router, actions = actions) }, content = content)
}