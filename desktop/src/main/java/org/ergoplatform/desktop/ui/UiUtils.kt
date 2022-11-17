package org.ergoplatform.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.compose.ComposePlatformUtils
import org.ergoplatform.compose.toComposableText
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.*

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.addOnEnterListener(onEnter: () -> Unit): Modifier {
    return this.onKeyEvent {
        if (it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
            onEnter()
            true
        } else false
    }
}

inline fun Modifier.noRippleClickable(crossinline onClick: () -> Unit): Modifier = composed {
    clickable(indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

fun showSensitiveDataCopyDialog(navHost: NavHostComponent, dataToCopy: String) {
    navHost.dialogHandler.showDialog(
        MosaikDialog(
            Application.texts.getString(STRING_DESC_COPY_SENSITIVE_DATA),
            Application.texts.getString(STRING_BUTTON_COPY_SENSITIVE_DATA),
            Application.texts.getString(STRING_LABEL_CANCEL),
            { dataToCopy.copyToClipboard() },
            null
        )
    )
}

@Composable
fun ergoLogo() = painterResource("symbol_bold__1080px__black.svg")

@Composable
fun ErgoAmount.toComposableText(trimTrailingZeros: Boolean = false) =
    toComposableText(Application.texts, trimTrailingZeros)

fun MessageSeverity.getSeverityIcon(): ImageVector? =
    when (this) {
        MessageSeverity.NONE -> null
        MessageSeverity.INFORMATION -> Icons.Default.Info
        MessageSeverity.WARNING -> Icons.Default.Warning
        MessageSeverity.ERROR -> Icons.Default.Error
    }

fun initComposePlatformUtils() {
    ComposePlatformUtils.getDrawablePainter = {
        painterResource(
            when (it) {
                ComposePlatformUtils.Drawable.Octagon -> "ic_octagon_48.xml"
                ComposePlatformUtils.Drawable.NftImage -> "ic_photo_camera_24.xml"
                ComposePlatformUtils.Drawable.NftAudio -> "ic_music_note_24.xml"
                ComposePlatformUtils.Drawable.NftVideo -> "ic_videocam_24.xml"
            }
        )
    }
}