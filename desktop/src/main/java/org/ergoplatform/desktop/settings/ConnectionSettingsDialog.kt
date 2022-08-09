package org.ergoplatform.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.ConnectionSettingsLayout
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_CONNECTION_SETTINGS

@Composable
fun ConnectionSettingsDialog(onDismissRequest: () -> Unit) {
    AppDialog({}) {
        Column(Modifier.fillMaxWidth().padding(defaultPadding)) {
            Row(Modifier.padding(bottom = defaultPadding).fillMaxWidth()) {
                Text(
                    Application.texts.getString(STRING_BUTTON_CONNECTION_SETTINGS),
                    Modifier.align(Alignment.CenterVertically).weight(1f),
                    style = labelStyle(LabelStyle.BODY1)
                )
                IconButton(onDismissRequest) {
                    Icon(Icons.Default.Close, null)
                }
            }

            ConnectionSettingsLayout(Application.prefs, Application.texts, onDismissRequest)
        }
    }
}