package org.ergoplatform.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import org.ergoplatform.mosaik.MiddleEllipsisText

@Composable
fun ErgoAddressText(
    address: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color
) {
    var showFull by remember { mutableStateOf(false) }
    val myModifier = modifier.clickable { showFull = !showFull }

    if (!showFull)
        MiddleEllipsisText(address, myModifier, style = style, color = color)
    else
        Text(address, myModifier, style = style, color = color)
}