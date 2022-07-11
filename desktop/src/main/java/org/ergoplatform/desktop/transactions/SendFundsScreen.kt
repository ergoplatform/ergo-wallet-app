package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*

@Composable
fun SendFundsScreen(
    walletConfig: WalletConfig,
    walletAddress: WalletAddress?,
    recipientAddress: MutableState<TextFieldValue>,
    amountToSend: MutableState<TextFieldValue>,
    recipientError: Boolean,
    amountError: Boolean,
    feeAmount: ErgoAmount,
    grossAmount: ErgoAmount,
    onMaxAmountClicked: () -> Unit,
    onChooseToken: () -> Unit,
) {
    AppScrollingLayout {
        Card(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {

                Text(
                    remember {
                        Application.texts.getString(
                            STRING_LABEL_SEND_FROM,
                            walletConfig.displayName ?: ""
                        )
                    },
                    style = labelStyle(LabelStyle.BODY1),
                )

                Text(
                    remember(walletAddress) { "Address name here" },
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor,
                    maxLines = 1,
                )

                Text(
                    remember(walletAddress) {
                        Application.texts.getString(
                            STRING_LABEL_WALLET_BALANCE, "????"
                        )
                    },
                    style = labelStyle(LabelStyle.BODY1),
                )

                // TODO read only wallet hint

                Text(
                    remember(walletAddress) { Application.texts.getString(STRING_DESC_SEND_FUNDS) },
                    Modifier.padding(top = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1),
                )

                OutlinedTextField(
                    recipientAddress.value,
                    onValueChange = {
                        recipientAddress.value = it
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding / 2),
                    singleLine = true,
                    isError = recipientError,
                    label = { Text(Application.texts.getString(STRING_LABEL_RECEIVER_ADDRESS)) },
                    colors = appTextFieldColors(),
                )

                OutlinedTextField(
                    amountToSend.value,
                    onValueChange = {
                        amountToSend.value = it
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding),
                    singleLine = true,
                    isError = amountError,
                    label = { Text(Application.texts.getString(STRING_LABEL_AMOUNT)) },
                    trailingIcon = {
                        IconButton(onClick = onMaxAmountClicked) {
                            Icon(Icons.Default.ArrowCircleDown, null)
                        }
                    },
                    colors = appTextFieldColors(),
                )

                Row(Modifier.align(Alignment.End)) {
                    Text(
                        remember(amountToSend.value) {
                            Application.texts.getString(
                                STRING_LABEL_FIAT_AMOUNT
                            )
                        },
                        style = labelStyle(LabelStyle.BODY1),
                        color = MosaikStyleConfig.secondaryLabelColor,
                    )
                    Icon(
                        Icons.Default.SyncAlt,
                        null,
                        Modifier.padding(start = defaultPadding / 4),
                        tint = MosaikStyleConfig.secondaryLabelColor
                    )
                }

                Text(
                    remember(feeAmount) { Application.texts.getString(STRING_DESC_FEE, "????") },
                    Modifier.padding(vertical = defaultPadding / 2),
                    style = labelStyle(LabelStyle.BODY1),
                )

                Text(
                    remember(grossAmount) {
                        Application.texts.getString(
                            STRING_LABEL_ERG_AMOUNT, grossAmount.toStringRoundToDecimals()
                        )
                    },
                    Modifier.padding(vertical = defaultPadding).fillMaxWidth(),
                    style = labelStyle(LabelStyle.HEADLINE1),
                    textAlign = TextAlign.Center,
                )

                // TODO tokens list
                // TODO token amount error

                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        // TODO add token button
                    }

                    Box(Modifier.weight(1f)) {
                        Button(
                            {},
                            Modifier.align(Alignment.CenterEnd),
                        ) {
                            Icon(
                                Icons.Default.Send,
                                null,
                                Modifier.padding(end = defaultPadding / 2)
                            )
                            Text(remember { Application.texts.getString(STRING_BUTTON_SEND) })
                        }
                    }
                }
            }
        }
    }
}