package org.ergoplatform.desktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import org.ergoplatform.mosaik.MiddleEllipsisText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErgoAddressText(
    address: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var showFull by remember { mutableStateOf(false) }
    val myModifier = modifier.combinedClickable(
        onClick = { showFull = !showFull },
        onLongClick = {
            clipboard.setText(AnnotatedString(address))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )

    if (!showFull)
        MiddleEllipsisText(address, myModifier, style = style, color = color)
    else
        Text(address, myModifier, style = style, color = color)
}