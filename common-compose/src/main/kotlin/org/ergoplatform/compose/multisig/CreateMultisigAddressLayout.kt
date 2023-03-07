package org.ergoplatform.compose.multisig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ergoplatform.appkit.Address
import org.ergoplatform.compose.settings.*
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.multisig.CreateMultisigAddressUiLogic
import kotlin.math.max

@Composable
fun CreateMultisigAddressLayout(
    modifier: Modifier,
    texts: StringProvider,
    uiLogic: CreateMultisigAddressUiLogic,
    participantAddress: MutableState<TextFieldValue>,
    onScanAddress: () -> Unit,
    onBack: (() -> Unit)?,
    onProceed: () -> Unit,
    getDb: () -> IAppDatabase,
) {
    AppCard(
        modifier.padding(defaultPadding)
            .defaultMinSize(400.dp, 200.dp)
            .widthIn(max = defaultMaxWidth)
    ) {

        Column(Modifier.padding(defaultPadding)) {
            Text(
                texts.getString(STRING_LABEL_MULTISIG_WALLET),
                style = labelStyle(LabelStyle.HEADLINE2),
                color = MosaikStyleConfig.primaryLabelColor
            )

            Text(
                texts.getString(STRING_INTRO_CREATE_MULTISIG),
                Modifier.padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1)
            )

            Spacer(Modifier.size(defaultPadding))

            var numberParticants by rememberSaveable { mutableStateOf(uiLogic.minSignersNeeded) }
            var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
            val participants = remember { mutableStateListOf<Address>() }

            val onAdd: () -> Unit = {
                try {
                    uiLogic.addParticipantAddress(participantAddress.value.text, texts)
                    participantAddress.value = TextFieldValue()
                    participants.clear()
                    participants.addAll(uiLogic.participants)
                } catch (t: Throwable) {
                    errorMsg = t.message
                }
            }

            Column(Modifier.animateContentSize()) {
                OutlinedTextField(
                    participantAddress.value,
                    onValueChange = { participantAddress.value = it },
                    Modifier.fillMaxWidth(),
                    maxLines = 1,
                    singleLine = true,
                    isError = errorMsg != null,
                    label = { Text(remember { texts.getString(STRING_LABEL_SIGNER_P2PK_ADDRESS) }) },
                    trailingIcon = {
                        IconButton(onClick = onScanAddress) {
                            Icon(Icons.Default.QrCodeScanner, null)
                        }
                    },
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                )

                errorMsg?.let {
                    Text(
                        it,
                        style = labelStyle(LabelStyle.BODY2),
                        color = MosaikStyleConfig.primaryLabelColor
                    )
                }
            }

            Row(Modifier.align(Alignment.End)) {
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
            }

            Column(Modifier.animateContentSize().padding(defaultPadding)) {
                participants.forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiddleEllipsisText(
                            it.toString(),
                            Modifier.weight(1f).padding(vertical = defaultPadding / 2)
                        )
                        IconButton(onClick = {
                            uiLogic.removeParticipantAddress(it)
                            participants.clear()
                            participants.addAll(uiLogic.participants)
                        }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            }

            Text(remember { texts.getString(STRING_LABEL_NUM_SIGNERS) })

            Row(
                Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = {
                    numberParticants = max(numberParticants - 1, uiLogic.minSignersNeeded)
                }) { Icon(Icons.Default.Remove, null) }

                Text(numberParticants.toString(), style = labelStyle(LabelStyle.HEADLINE2))

                IconButton(onClick = {
                    numberParticants += 1
                }) { Icon(Icons.Default.Add, null) }

            }

            var walletName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(uiLogic.defaultWalletName(texts)))
            }

            val coroutineScope = rememberCoroutineScope()
            var addError by rememberSaveable { mutableStateOf("") }
            val onDone: () -> Unit = {
                coroutineScope.launch {
                    try {
                        uiLogic.addWalletToDb(
                            numberParticants,
                            getDb().walletDbProvider,
                            texts,
                            walletName.text,
                        )
                        onProceed()
                    } catch (t: Throwable) {
                        addError = t.message ?: "Error"
                    }
                }
            }

            OutlinedTextField(
                walletName,
                onValueChange = { walletName = it },
                Modifier.fillMaxWidth().padding(top = defaultPadding),
                maxLines = 1,
                singleLine = true,
                label = { Text(texts.getString(STRING_LABEL_WALLET_NAME)) },
                colors = appTextFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
            )

            AnimatedVisibility(visible = addError.isNotBlank()) {
                Text(
                    addError,
                    Modifier.align(Alignment.CenterHorizontally).padding(defaultPadding),
                    color = MosaikStyleConfig.primaryLabelColor,
                    textAlign = TextAlign.Center,
                )
            }

            Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                onBack?.let {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(end = defaultPadding),
                        colors = secondaryButtonColors()
                    ) {
                        Text(texts.getString(STRING_BUTTON_BACK))
                    }
                }

                Button(
                    onClick = onDone,
                    colors = primaryButtonColors()
                ) {
                    Text(texts.getString(STRING_BUTTON_CREATE_MULTISIG))
                }
            }

        }
    }
}