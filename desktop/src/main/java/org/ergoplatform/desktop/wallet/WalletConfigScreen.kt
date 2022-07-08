package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_BUTTON_APPLY
import org.ergoplatform.uilogic.STRING_LABEL_COPIED
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_NAME

@Composable
fun WalletConfigScreen(
    walletConfig: WalletConfig,
    scaffoldState: ScaffoldState?,
    onChangeName: suspend (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    AppScrollingLayout {
        Card(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {
            Column(Modifier.padding(defaultPadding)) {
                Row {
                    Text(
                        walletConfig.firstAddress!!,
                        style = labelStyle(LabelStyle.HEADLINE2),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            walletConfig.firstAddress?.copyToClipoard()
                            coroutineScope.launch {
                                scaffoldState?.snackbarHostState?.showSnackbar(
                                    Application.texts.getString(STRING_LABEL_COPIED),
                                )
                            }
                        },
                        Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                    }
                }

                val nameTextSate =
                    remember { mutableStateOf(TextFieldValue(walletConfig.displayName ?: "")) }

                OutlinedTextField(
                    nameTextSate.value,
                    { textFieldValue ->
                        nameTextSate.value = textFieldValue
                    },
                    Modifier.padding(top = defaultPadding * 1.5f).fillMaxWidth(),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(Application.texts.getString(STRING_LABEL_WALLET_NAME)) },
                    colors = appTextFieldColors(),
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            onChangeName(nameTextSate.value.text)
                        }
                    },
                    colors = primaryButtonColors(),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(Application.texts.getString(STRING_BUTTON_APPLY))
                }
            }
        }
    }
}