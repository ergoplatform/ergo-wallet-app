package org.ergoplatform.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.arkivanov.decompose.router.Router
import com.arkivanov.decompose.router.pop
import org.ergoplatform.desktop.ui.navigation.Component
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.mosaik.MosaikStyleConfig
import java.awt.event.KeyEvent

val uiErgoColor get() = if (isErgoMainNet) Color(0xffff3b30) else Color(0xff4284FF)
val secondaryColor = Color(24, 25, 29)
val defaultPadding = 16.dp
val defaultMaxWidth = 600.dp
val bigIconSize = 58.dp

private val DarkColors = darkColors(
    primary = uiErgoColor,
    secondary = uiErgoColor,
    surface = secondaryColor,
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
fun appTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = MaterialTheme.colors.onSurface,
    focusedLabelColor = MaterialTheme.colors.onSurface,
    errorLabelColor = uiErgoColor,
    errorBorderColor = uiErgoColor,
    errorTrailingIconColor = uiErgoColor,
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
                AppBackButton(onClick = router::pop)
            })
        )
    }
}

@Composable
fun AppBarView(
    title: String,
    actions: @Composable RowScope.() -> Unit,
    router: Router<ScreenConfig, Component>,
    bottombar: @Composable () -> Unit,
    content: @Composable (PaddingValues, ScaffoldState) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        topBar = { WalletAppBar(title, router, actions = actions) },
        scaffoldState = scaffoldState,
        content = { paddingValues -> content(paddingValues, scaffoldState) },
        bottomBar = bottombar
    )
}

@Composable
fun AppCard(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        content()
    }
}

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit, maxWidth: Dp = defaultMaxWidth,
    content: @Composable () -> Unit
) {
    Popup(
        onDismissRequest = onDismissRequest,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        },
        focusable = true,
        onKeyEvent = {
            if (it.awtEventOrNull?.keyCode == KeyEvent.VK_ESCAPE) {
                onDismissRequest()
                true
            } else {
                false
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onDismissRequest) {
                    detectTapGestures(onPress = { onDismissRequest() })
                }.background(MaterialTheme.colors.background.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            AppCard(
                modifier = Modifier.padding(defaultPadding)
                    .widthIn(min = 400.dp, max = maxWidth)
                    .heightIn(min = 200.dp)
                    .noRippleClickable {
                        // no action, prevents dismissal
                    }

            ) {
                content()
            }
        }
    }
}

@Composable
fun AppBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null
        )
    }
}

@Composable
fun AppScrollingLayout(content: @Composable BoxScope.() -> Unit) {
    Box {
        val scrollState = rememberScrollState()
        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            content()
        }

        AppScrollbar(scrollState)
    }
}

@Composable
fun BoxScope.AppScrollbar(scrollState: ScrollState) {
    VerticalScrollbar(
        modifier = Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight(),
        adapter = rememberScrollbarAdapter(scrollState)
    )
}

@Composable
fun AppLockScreen(locked: Boolean) {
    if (locked)
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f))
                .noRippleClickable {
                    // needed to grab user interaction
                }) {
            CircularProgressIndicator(
                Modifier.size(48.dp).align(Alignment.Center),
                color = uiErgoColor
            )
        }
}