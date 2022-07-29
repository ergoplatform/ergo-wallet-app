package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.getAddressDerivationPath
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.*

@Composable
fun WalletAddressDetailsDialog(
    address: WalletAddress,
    scaffoldState: ScaffoldState?,
    onChangeName: (WalletAddress, String) -> Unit,
    onDeleteAddress: (WalletAddress) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    AppDialog(onDismiss) {

        Column(Modifier.fillMaxWidth().padding(defaultPadding)) {

            Icon(Icons.Default.Close, null, Modifier.align(Alignment.End).clickable { onDismiss() })

            Row {
                Text(
                    text = address.publicAddress,
                    style = labelStyle(LabelStyle.HEADLINE2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        address.publicAddress.copyToClipboard()
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

            Text(
                text = remember {
                    getAddressDerivationPath(address.derivationIndex)
                },
                style = labelStyle(LabelStyle.BODY1BOLD),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = defaultPadding / 2),
            )

            Text(
                text = remember { Application.texts.getString(STRING_DESC_WALLET_ADDR_LABEL) },
                style = labelStyle(LabelStyle.BODY1),
                modifier = Modifier.fillMaxWidth().padding(top = defaultPadding),
            )

            val nameTextSate =
                remember { mutableStateOf(TextFieldValue(address.label ?: "")) }

            OutlinedTextField(
                nameTextSate.value,
                { textFieldValue ->
                    nameTextSate.value = textFieldValue
                },
                Modifier.padding(vertical = defaultPadding / 2).fillMaxWidth(),
                maxLines = 1,
                singleLine = true,
                label = { Text(remember { Application.texts.getString(STRING_LABEL_WALLET_NAME) }) },
                colors = appTextFieldColors(),
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        onChangeName(address, nameTextSate.value.text)
                    }
                },
                colors = primaryButtonColors(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(remember { Application.texts.getString(STRING_BUTTON_APPLY) })
            }

            Text(
                text = remember { Application.texts.getString(STRING_DESC_WALLET_ADDR_REMOVE) },
                style = labelStyle(LabelStyle.BODY1),
                modifier = Modifier.fillMaxWidth().padding(top = defaultPadding),
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        onDeleteAddress(address)
                    }
                },
                colors = primaryButtonColors(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(remember { Application.texts.getString(STRING_LABEL_REMOVE) })
            }

        }
    }
}