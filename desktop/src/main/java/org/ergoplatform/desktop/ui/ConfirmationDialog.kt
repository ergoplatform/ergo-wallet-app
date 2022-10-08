package org.ergoplatform.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_LABEL_CANCEL

@Composable
fun ConfirmationDialog(
    confirmButtonText: String,
    confirmationPrompt: String,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    AppDialog({}) {
        Column(Modifier.fillMaxWidth().padding(defaultPadding)) {

            Text(confirmationPrompt, style = labelStyle(LabelStyle.HEADLINE2))

            Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                Button(
                    onClick = {
                        onDismissRequest()
                    },
                    colors = secondaryButtonColors(),
                    modifier = Modifier.padding(end = defaultPadding * 2),
                ) {
                    Text(Application.texts.getString(STRING_LABEL_CANCEL))
                }
                Button(
                    onClick = {
                        onDismissRequest()
                        onConfirmation()
                    },
                    colors = primaryButtonColors(),
                ) {
                    Text(confirmButtonText)
                }
            }
        }
    }
}