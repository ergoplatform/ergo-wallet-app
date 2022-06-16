package org.ergoplatform.desktop.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.pop
import org.ergoplatform.desktop.ui.navigation.Component
import org.ergoplatform.desktop.ui.navigation.ScreenConfig

val uiErgoColor = Color(0xff, 0x45, 0x3a)
val secondary = Color(24, 25, 29)
val tsBody1 = TextStyle(fontSize = 18.sp)
val tsHeadline1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)

private val DarkColors = darkColors(
    primary = uiErgoColor,
    secondary = uiErgoColor,
    surface = secondary,
    onPrimary = Color.White,
    onSecondary = Color.White
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
private fun WalletAppBar(title: String, router: Router<ScreenConfig, Component>) {
    CompositionLocalProvider(LocalElevationOverlay provides null) {
        TopAppBar(
            backgroundColor = MaterialTheme.colors.primary,
            title = { Text(title) },
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
    router: Router<ScreenConfig, Component>,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(topBar = { WalletAppBar(title, router) }, content = content)
}