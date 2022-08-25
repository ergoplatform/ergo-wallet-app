package org.ergoplatform.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
import androidx.compose.ui.text.style.TextAlign
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_IMAGE_FROM_CLIPBOARD
import org.ergoplatform.uilogic.STRING_INPUT_ENTER_OR_PASTE_MANUALLY

@Composable
fun QrScannerScreen(
    imageState: MutableState<ImageBitmap?>,
    errorState: MutableState<String>,
    scannedQr: (String) -> Unit,
    pasteImage: () -> Unit,
    dismiss: () -> Unit,
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
                Modifier.align(Alignment.Center).padding(defaultPadding * 3, defaultPadding),
                style = labelStyle(LabelStyle.HEADLINE2),
                textAlign = TextAlign.Center,
            )

            AppBackButton(onClick = dismiss)
        }
        Button(
            onClick = pasteImage,
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding)
        ) {
            Text(Application.texts.getString(STRING_BUTTON_IMAGE_FROM_CLIPBOARD))
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
            label = { Text(Application.texts.getString(STRING_INPUT_ENTER_OR_PASTE_MANUALLY)) },
            trailingIcon = {
                IconButton(onClick = onClickOrEnter) {
                    Icon(Icons.Default.KeyboardReturn, null)
                }
            },
            colors = appTextFieldColors(),
        )
    }
}
