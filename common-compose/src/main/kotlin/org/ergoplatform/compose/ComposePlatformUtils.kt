package org.ergoplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Handler for code that needs to be different on Android and on Desktop
 */
object ComposePlatformUtils {
    lateinit var getDrawablePainter: @Composable (Drawable) -> Painter

    enum class Drawable {
        Octagon,
        NftImage,
        NftAudio,
        NftVideo,
    }
}