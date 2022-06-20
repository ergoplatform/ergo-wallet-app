package org.ergoplatform.desktop.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.addOnEnterListener(onEnter: () -> Unit): Modifier {
    return this.onKeyEvent {
        if (it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
            onEnter()
            true
        } else false
    }
}