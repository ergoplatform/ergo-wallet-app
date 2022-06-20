package org.ergoplatform.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

@Composable
fun QrScannerScreen(
    imageState: MutableState<ImageBitmap?>,
    errorState: MutableState<String>,
    scannedQr: (String) -> Unit
) {

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
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

        val manualTextFieldValue = remember { mutableStateOf(TextFieldValue()) }

        val onClickOrEnter = {
            val text = manualTextFieldValue.value.text
            if (text.isNotBlank())
                scannedQr(text)
        }

        OutlinedTextField(
            manualTextFieldValue.value,
            onValueChange = {
                manualTextFieldValue.value = it
            },
            Modifier.padding(defaultPadding).fillMaxWidth().addOnEnterListener(onClickOrEnter),
            maxLines = 1,
            singleLine = true,
            label = { Text("or enter/paste manually") },
            trailingIcon = {
                IconButton(onClick = onClickOrEnter) {
                    Icon(Icons.Default.KeyboardReturn, null)
                }
            },
        )
    }
}
