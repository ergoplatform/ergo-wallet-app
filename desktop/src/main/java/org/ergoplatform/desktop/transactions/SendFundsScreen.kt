package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.URL_COLD_WALLET_HELP
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressButton
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.wallet.isReadOnly

@Composable
fun SendFundsScreen(
    walletConfig: WalletConfig,
    walletAddress: WalletAddress?,
    recipientAddress: MutableState<TextFieldValue>,
    purposeMessage: MutableState<TextFieldValue>,
    amountToSend: MutableState<TextFieldValue>,
    amountsChangedCount: Int,
    recipientError: MutableState<Boolean>,
    amountError: MutableState<Boolean>,
    tokensChosen: List<String>,
    tokensError: MutableState<Boolean>,
    uiLogic: SendFundsUiLogic,
    onChooseAddressClicked: () -> Unit,
    onChooseToken: () -> Unit,
    onSendClicked: () -> Unit,
    onChooseFeeClicked: () -> Unit,
    onChooseRecipientAddress: () -> Unit,
    onPurposeMessageInfoClicked: () -> Unit,
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

                ChooseAddressButton(
                    walletAddress,
                    uiLogic.wallet,
                    onClick = onChooseAddressClicked
                )

                Text(
                    remember(amountsChangedCount) {
                        Application.texts.getString(
                            STRING_LABEL_WALLET_BALANCE,
                            uiLogic.balance.toStringRoundToDecimals()
                        )
                    },
                    style = labelStyle(LabelStyle.BODY1),
                )

                if (walletConfig.isReadOnly())
                    Box(Modifier.padding(top = defaultPadding)) {
                        Card(modifier = Modifier.border(1.dp, uiErgoColor)) {
                            LinkifyText(
                                remember {
                                    Application.texts.getString(STRING_HINT_READ_ONLY)
                                        .replace("href=\"\"", "href=\"$URL_COLD_WALLET_HELP\"")
                                },
                                Modifier.padding(defaultPadding / 2),
                                labelStyle(LabelStyle.BODY1),
                                uiErgoColor,
                                true
                            )
                        }
                    }

                Text(
                    remember { Application.texts.getString(STRING_DESC_SEND_FUNDS) },
                    Modifier.padding(top = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1),
                )

                val recipientAddressString = recipientAddress.value.text
                val addressLabelState = remember(recipientAddressString) {
                    mutableStateOf<String?>(null)
                }
                LaunchedEffect(recipientAddressString) {
                    getAddressLabelFromDatabase(
                        Application.database,
                        recipientAddressString,
                        Application.texts
                    )?.let {
                        addressLabelState.value = it
                    }
                }

                val readOnly = addressLabelState.value != null

                OutlinedTextField(
                    addressLabelState.value?.let { TextFieldValue(it) } ?: recipientAddress.value,
                    onValueChange = {
                        if (!readOnly) {
                            recipientAddress.value = it
                            recipientError.value = false
                            uiLogic.receiverAddress = it.text
                        }
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding / 2),
                    singleLine = true,
                    isError = recipientError.value,
                    label = { Text(Application.texts.getString(STRING_LABEL_RECEIVER_ADDRESS)) },
                    colors = appTextFieldColors(),
                    trailingIcon = {
                        if (readOnly)
                            IconButton(onClick = {
                                recipientAddress.value = TextFieldValue()
                                uiLogic.receiverAddress = ""
                            }) { Icon(Icons.Default.Close, null) }
                        else
                            IconButton(onClick = onChooseRecipientAddress) {
                                Icon(Icons.Default.PermContactCalendar, null)
                            }
                    },
                    readOnly = readOnly,
                )

                OutlinedTextField(
                    purposeMessage.value,
                    onValueChange = {
                        purposeMessage.value = it
                        uiLogic.message = it.text
                    },
                    Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(remember { Application.texts.getString(STRING_LABEL_PURPOSE) }) },
                    trailingIcon = {
                        IconButton(onClick = onPurposeMessageInfoClicked) {
                            Icon(Icons.Outlined.Info, null)
                        }
                    },
                    colors = appTextFieldColors(),
                )

                OutlinedTextField(
                    amountToSend.value,
                    onValueChange = {
                        amountToSend.value = it
                        amountError.value = false
                        uiLogic.inputAmountChanged(it.text)
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding),
                    singleLine = true,
                    isError = amountError.value,
                    label = {
                        Text(
                            remember(uiLogic.inputIsFiat) {
                                if (uiLogic.inputIsFiat) Application.texts.getString(
                                    STRING_HINT_AMOUNT_CURRENCY,
                                    WalletStateSyncManager.getInstance().fiatCurrency.uppercase()
                                ) else Application.texts.getString(STRING_LABEL_AMOUNT)
                            })
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            uiLogic.setAmountToSendErg(uiLogic.getMaxPossibleAmountToSend())
                            amountToSend.value = TextFieldValue(uiLogic.inputAmountString)
                        }) {
                            Icon(Icons.Default.ArrowCircleDown, null)
                        }
                    },
                    colors = appTextFieldColors(),
                )

                val otherCurrency =
                    remember(amountsChangedCount) { uiLogic.getOtherCurrencyLabel(Application.texts) }

                if (otherCurrency != null)
                    Row(Modifier.align(Alignment.End).clickable {
                        val changed = uiLogic.switchInputAmountMode()
                        if (changed) {
                            Application.prefs.isSendInputFiatAmount = uiLogic.inputIsFiat
                            amountToSend.value = TextFieldValue(uiLogic.inputAmountString)
                            uiLogic.notifyAmountsChanged()
                        }
                    }) {
                        Text(
                            otherCurrency,
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
                    remember(amountsChangedCount) { uiLogic.getFeeDescriptionLabel(Application.texts) },
                    Modifier.padding(vertical = defaultPadding / 2).clickable {
                        onChooseFeeClicked()
                    },
                    style = labelStyle(LabelStyle.BODY1),
                )

                Text(
                    remember(amountsChangedCount) {
                        Application.texts.getString(
                            STRING_LABEL_ERG_AMOUNT,
                            uiLogic.grossAmount.toStringRoundToDecimals()
                        )
                    },
                    Modifier.padding(vertical = defaultPadding).fillMaxWidth(),
                    style = labelStyle(LabelStyle.HEADLINE1),
                    textAlign = TextAlign.Center,
                )

                tokensChosen.forEach { tokenId ->
                    uiLogic.tokensAvail[tokenId]?.let { tokenDbEntity ->
                        key(tokenDbEntity.tokenId) {
                            SendTokenItem(
                                tokenDbEntity,
                                tokensError,
                                uiLogic,
                                onRemove = { uiLogic.removeToken(tokenDbEntity.tokenId!!) }
                            )
                        }
                    }
                }
                if (tokensError.value) {
                    Text(
                        remember { Application.texts.getString(STRING_ERROR_TOKEN_AMOUNT) },
                        Modifier.fillMaxWidth().padding(horizontal = defaultPadding / 2),
                        color = uiErgoColor,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(Modifier.fillMaxWidth().padding(top = defaultPadding / 2)) {
                    Box(Modifier.weight(1f)) {
                        if ((uiLogic.tokensChosen.size < uiLogic.tokensAvail.size))
                            Button(
                                onChooseToken,
                                Modifier.align(Alignment.CenterStart),
                                colors = secondaryButtonColors(),
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    null,
                                    Modifier.padding(end = defaultPadding / 2)
                                )
                                Text(remember { Application.texts.getString(STRING_LABEL_ADD_TOKEN) })
                            }
                    }

                    Box(Modifier.weight(1f)) {
                        Button(
                            onSendClicked,
                            Modifier.align(Alignment.CenterEnd),
                            colors = primaryButtonColors(),
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