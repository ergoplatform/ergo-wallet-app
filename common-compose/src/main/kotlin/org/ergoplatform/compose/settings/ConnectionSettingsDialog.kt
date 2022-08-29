package org.ergoplatform.compose.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.foregroundColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.settings.SettingsUiLogic

@Composable
fun ColumnScope.ConnectionSettingsLayout(
    uiLogic: SettingsUiLogic,
    onStartNodeDetection: () -> Unit,
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

    val checkNodesState = uiLogic.checkNodesState.collectAsState()
    val showList = remember { mutableStateOf(false) }

    if (checkNodesState.value == SettingsUiLogic.CheckNodesState.Waiting) {

        ConnectionTextField(nodeApiUrl, stringProvider, STRING_LABEL_NODE_URL, trailingIcon = {
            IconButton(onClick = {
                onStartNodeDetection()
                showList.value = true
            }) {
                Icon(Icons.Default.AutoFixHigh, null)
            }
        })

        if (uiLogic.lastNodeList.isNotEmpty() && showList.value) {

            Column(
                Modifier.align(Alignment.CenterHorizontally)
                    .padding(horizontal = defaultPadding * 2)
                    .padding(bottom = defaultPadding)
            ) {
                Text(remember { stringProvider.getString(STRING_LABEL_NODE_ALTERNATIVES) })

                uiLogic.lastNodeList.forEach { nodeInfo ->
                    NodeInfoEntry(onChooseNode = {
                        nodeApiUrl.value = TextFieldValue(nodeInfo.nodeUrl)
                        showList.value = false
                    }, nodeInfo, stringProvider)
                }
            }

        } else if (showList.value) {
            Row(Modifier.padding(bottom = defaultPadding).padding(horizontal = defaultPadding)) {

                Icon(Icons.Default.Error, null, tint = MosaikStyleConfig.primaryLabelColor)

                Text(remember { stringProvider.getString(STRING_LABEL_NODE_NONE_FOUND) }, Modifier.padding(start = defaultPadding / 2))

            }
        }
    } else {
        CheckNodeStateView(checkNodesState, stringProvider)
    }

    ConnectionTextField(tokenVerificationUrl, stringProvider, STRING_LABEL_TOKEN_VERIFICATION_URL)

    ConnectionTextField(
        ipfsGatewayUrl,
        stringProvider,
        STRING_LABEL_IPFS_HTTP_GATEWAY,
        applySettings
    )

    Row(Modifier.align(Alignment.End)) {
        AppButton(
            onClick = {
                explorerApiUrl.value = TextFieldValue(getDefaultExplorerApiUrl())
                nodeApiUrl.value = TextFieldValue(preferences.getDefaultNodeApiUrl())
                tokenVerificationUrl.value =
                    TextFieldValue(preferences.defaultTokenVerificationUrl)
                ipfsGatewayUrl.value =
                    TextFieldValue(preferences.defaultIpfsGatewayUrl)
            },
            colors = secondaryButtonColors(),
            modifier = Modifier.padding(end = defaultPadding),
        ) {
            Text(remember { stringProvider.getString(STRING_BUTTON_RESET_DEFAULTS) })
        }
        AppButton(
            onClick = applySettings,
            colors = primaryButtonColors(),
        ) {
            Text(remember { stringProvider.getString(STRING_BUTTON_APPLY) })
        }
    }
}

@Composable
private fun ColumnScope.CheckNodeStateView(
    checkNodesState: State<SettingsUiLogic.CheckNodesState>,
    stringProvider: StringProvider
) {
    Row(
        Modifier.padding(horizontal = defaultPadding).padding(bottom = defaultPadding)
            .align(Alignment.CenterHorizontally).heightIn(min = bigIconSize)
    ) {
        AppProgressIndicator(
            Modifier.align(Alignment.CenterVertically).padding(end = defaultPadding / 2),
            smallIconSize
        )
        when (val checkNodeCurrentState = checkNodesState.value) {
            SettingsUiLogic.CheckNodesState.FetchingNodes ->
                Text(
                    remember { stringProvider.getString(STRING_LABEL_FETCHING_NODE_LIST) },
                    Modifier.align(Alignment.CenterVertically),
                    style = labelStyle(LabelStyle.BODY1LINK),
                    color = foregroundColor(ForegroundColor.SECONDARY)
                )
            is SettingsUiLogic.CheckNodesState.TestingNode ->
                Text(
                    remember(checkNodeCurrentState.nodeUrl) {
                        stringProvider.getString(
                            STRING_LABEL_CHECKING_NODE,
                            checkNodeCurrentState.nodeUrl
                        )
                    },
                    Modifier.align(Alignment.CenterVertically),
                    style = labelStyle(LabelStyle.BODY1),
                    color = foregroundColor(ForegroundColor.SECONDARY)
                )
            SettingsUiLogic.CheckNodesState.Waiting -> {}
        }
    }
}

@Composable
private fun NodeInfoEntry(
    onChooseNode: () -> Unit,
    nodeInfo: SettingsUiLogic.NodeInfo,
    stringProvider: StringProvider
) {
    Surface(
        Modifier.padding(vertical = defaultPadding / 4).fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MosaikStyleConfig.secondaryLabelColor)
    ) {
        Column(Modifier.clickable { onChooseNode() }.padding(defaultPadding / 2)) {
            Text(nodeInfo.nodeUrl, style = labelStyle(LabelStyle.BODY1))
            Text(
                stringProvider.getString(
                    STRING_LABEL_NODE_INFO,
                    nodeInfo.blockHeight,
                    nodeInfo.responseTime
                ),
                style = labelStyle(LabelStyle.BODY2),
                color = foregroundColor(ForegroundColor.SECONDARY),
            )
        }
    }
}

@Composable
private fun ConnectionTextField(
    explorerApiUrl: MutableState<TextFieldValue>,
    stringProvider: StringProvider,
    label: String,
    apply: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        explorerApiUrl.value,
        { textFieldValue ->
            explorerApiUrl.value = textFieldValue
        },
        Modifier.padding(bottom = defaultPadding).fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = if (apply == null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = apply?.let { { apply() } }),
        maxLines = 1,
        singleLine = true,
        label = { Text(stringProvider.getString(label)) },
        colors = appTextFieldColors(),
        trailingIcon = trailingIcon
    )
}
