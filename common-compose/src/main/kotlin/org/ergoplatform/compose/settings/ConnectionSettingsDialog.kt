package org.ergoplatform.compose.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.uilogic.*

@Composable
fun ColumnScope.ConnectionSettingsLayout(
    preferences: PreferencesProvider,
    stringProvider: StringProvider,
    onDismissRequest: () -> Unit
) {
    val explorerApiUrl =
        remember { mutableStateOf(TextFieldValue(preferences.prefExplorerApiUrl)) }
    val nodeApiUrl =
        remember { mutableStateOf(TextFieldValue(preferences.prefNodeUrl)) }
    val tokenVerificationUrl =
        remember { mutableStateOf(TextFieldValue(preferences.prefTokenVerificationUrl)) }
    val ipfsGatewayUrl =
        remember { mutableStateOf(TextFieldValue(preferences.prefIpfsGatewayUrl)) }

    val applySettings = {
        preferences.prefExplorerApiUrl = explorerApiUrl.value.text
        preferences.prefNodeUrl = nodeApiUrl.value.text
        preferences.prefIpfsGatewayUrl = ipfsGatewayUrl.value.text
        preferences.prefTokenVerificationUrl = tokenVerificationUrl.value.text

        // reset api service of NodeConnector to load new settings
        ApiServiceManager.resetApiService()

        onDismissRequest()
    }

    ConnectionTextField(explorerApiUrl, stringProvider, STRING_LABEL_EXPLORER_API_URL)

    ConnectionTextField(nodeApiUrl, stringProvider, STRING_LABEL_NODE_URL)

    ConnectionTextField(tokenVerificationUrl, stringProvider, STRING_LABEL_TOKEN_VERIFICATION_URL)

    ConnectionTextField(
        ipfsGatewayUrl,
        stringProvider,
        STRING_LABEL_IPFS_HTTP_GATEWAY,
        applySettings
    )

    Row(Modifier.align(Alignment.End)) {
        Button(
            onClick = {
                explorerApiUrl.value = TextFieldValue(getDefaultExplorerApiUrl())
                nodeApiUrl.value = TextFieldValue(preferences.getDefaultNodeApiUrl())
                tokenVerificationUrl.value =
                    TextFieldValue(preferences.defaultTokenVerificationUrl)
                ipfsGatewayUrl.value =
                    TextFieldValue(preferences.defaultIpfsGatewayUrl)
            },
            colors = secondaryButtonColors(),
            modifier = Modifier.padding(end = defaultPadding * 2),
        ) {
            Text(remember { stringProvider.getString(STRING_BUTTON_RESET_DEFAULTS) })
        }
        Button(
            onClick = applySettings,
            colors = primaryButtonColors(),
        ) {
            Text(remember { stringProvider.getString(STRING_ZXING_BUTTON_OK) })
        }
    }
}

@Composable
private fun ConnectionTextField(
    explorerApiUrl: MutableState<TextFieldValue>,
    stringProvider: StringProvider,
    label: String,
    apply: (() -> Unit)? = null
) {
    OutlinedTextField(
        explorerApiUrl.value,
        { textFieldValue ->
            explorerApiUrl.value = textFieldValue
        },
        Modifier.padding(bottom = defaultPadding).fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = if (apply == null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = apply?.let{ { apply() } }),
        maxLines = 1,
        singleLine = true,
        label = { Text(stringProvider.getString(label)) },
        colors = appTextFieldColors(),
    )
}
