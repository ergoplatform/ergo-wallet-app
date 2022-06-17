package org.ergoplatform.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

@Composable
fun QrScannerScreen(imageState: MutableState<ImageBitmap?>, errorState: MutableState<String>) {

    Box(Modifier.fillMaxSize()) {
        imageState.value?.let { image ->
            Image(
                image,
                null,
                Modifier.fillMaxSize()
            )
        }

        Text(
            errorState.value,
            Modifier.align(Alignment.Center),
            style = labelStyle(LabelStyle.HEADLINE2)
        )
    }
}
