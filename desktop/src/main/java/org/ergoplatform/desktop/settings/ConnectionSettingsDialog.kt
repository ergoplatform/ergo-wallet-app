package org.ergoplatform.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun ConnectionSettingsDialog(onDismissRequest: () -> Unit) {
    val explorerApiUrl =
        remember { mutableStateOf(TextFieldValue(Application.prefs.prefExplorerApiUrl)) }
    val nodeApiUrl =
        remember { mutableStateOf(TextFieldValue(Application.prefs.prefNodeUrl)) }
    val tokenVerificationUrl =
        remember { mutableStateOf(TextFieldValue(Application.prefs.prefTokenVerificationUrl)) }
    val ipfsGatewayUrl =
        remember { mutableStateOf(TextFieldValue(Application.prefs.prefIpfsGatewayUrl)) }

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

            ConnectionTextField(explorerApiUrl, STRING_LABEL_EXPLORER_API_URL)

            ConnectionTextField(nodeApiUrl, STRING_LABEL_NODE_URL)

            ConnectionTextField(tokenVerificationUrl, STRING_LABEL_TOKEN_VERIFICATION_URL)

            ConnectionTextField(ipfsGatewayUrl, STRING_LABEL_IPFS_HTTP_GATEWAY)

            Row(Modifier.align(Alignment.End)) {
                Button(
                    onClick = {
                        explorerApiUrl.value = TextFieldValue(getDefaultExplorerApiUrl())
                        nodeApiUrl.value = TextFieldValue(Application.prefs.getDefaultNodeApiUrl())
                        tokenVerificationUrl.value =
                            TextFieldValue(Application.prefs.defaultTokenVerificationUrl)
                        ipfsGatewayUrl.value =
                            TextFieldValue(Application.prefs.defaultIpfsGatewayUrl)
                    },
                    colors = secondaryButtonColors(),
                    modifier = Modifier.padding(end = defaultPadding * 2),
                ) {
                    Text(Application.texts.getString(STRING_BUTTON_RESET_DEFAULTS))
                }
                Button(
                    onClick = {
                        val preferences = Application.prefs
                        preferences.prefExplorerApiUrl = explorerApiUrl.value.text
                        preferences.prefNodeUrl = nodeApiUrl.value.text
                        preferences.prefIpfsGatewayUrl = ipfsGatewayUrl.value.text
                        preferences.prefTokenVerificationUrl = tokenVerificationUrl.value.text

                        // reset api service of NodeConnector to load new settings
                        ApiServiceManager.resetApiService()

                        onDismissRequest()
                    },
                    colors = primaryButtonColors(),
                ) {
                    Text(Application.texts.getString(STRING_ZXING_BUTTON_OK))
                }
            }
        }
    }
}

@Composable
private fun ConnectionTextField(
    explorerApiUrl: MutableState<TextFieldValue>,
    label: String
) {
    OutlinedTextField(
        explorerApiUrl.value,
        { textFieldValue ->
            explorerApiUrl.value = textFieldValue
        },
        Modifier.padding(bottom = defaultPadding).fillMaxWidth(),
        maxLines = 1,
        singleLine = true,
        label = { Text(Application.texts.getString(label)) },
        colors = appTextFieldColors(),
    )
}
