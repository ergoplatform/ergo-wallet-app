package org.ergoplatform.desktop.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.ergoplatform.desktop.ui.AppScrollbar
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_CONNECTION_SETTINGS
import org.ergoplatform.uilogic.settings.SettingsUiLogic

@Composable
fun ConnectionSettingsDialog(
    uiLogic: SettingsUiLogic,
    onStartNodeDetection: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AppDialog({}) {
        val scrollState = rememberScrollState()
        Column(Modifier.fillMaxWidth().padding(defaultPadding).verticalScroll(scrollState)) {
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

            ConnectionSettingsLayout(
                uiLogic,
                onStartNodeDetection,
                Application.prefs,
                Application.texts,
                onDismissRequest
            )
        }
    }
}