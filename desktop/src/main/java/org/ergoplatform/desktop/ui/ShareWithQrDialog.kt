package org.ergoplatform.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_DONE

@Composable
fun ShareWithQrDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    val qrImage = remember(text) { getQrCodeImageBitmap(text) }

    AppDialog(onDismiss) {
        Column(Modifier.padding(defaultPadding)) {

            Image(
                qrImage,
                null,
                Modifier.size(400.dp).padding(defaultPadding)
                    .align(Alignment.CenterHorizontally)
            )

            Row {
                Text(
                    text = text,
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = { text.copyToClipboard() },
                    Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                }
            }
            Button(
                onClick = onDismiss,
                colors = primaryButtonColors(),
                modifier = Modifier.padding(top = defaultPadding * 1.5f).align(Alignment.End)
            ) {
                Text(Application.texts.getString(STRING_BUTTON_DONE))
            }
        }
    }
}