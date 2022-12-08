package org.ergoplatform.compose.transactions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.toComposableText
import org.ergoplatform.desktop.tokens.TokenEntryView
import org.ergoplatform.desktop.ui.ErgoAddressText
import org.ergoplatform.explorer.client.model.AdditionalRegisters
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic

@Composable
fun SignTransactionInfoLayout(
    modifier: Modifier,
    origTransactionInfo: TransactionInfo,
    onConfirm: () -> Unit,
    onTokenClick: ((String) -> Unit)?,
    texts: StringProvider,
    getDb: () -> IAppDatabase,
) {

    val reducedViewState = rememberSaveable(origTransactionInfo) { mutableStateOf(true) }
    val showReduced = reducedViewState.value
    val transactionInfo = remember(origTransactionInfo, showReduced) {
        if (showReduced)
            origTransactionInfo.reduceBoxes()
        else
            origTransactionInfo
    }

    Column(modifier) {

        Text(
            remember { texts.getString(STRING_DESC_SIGNING_REQUEST) },
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY1BOLD),
        )

        transactionInfo.hintMsg?.let { (message, severity) ->
            val warning = severity == MessageSeverity.WARNING || severity == MessageSeverity.ERROR
            Text(
                message,
                Modifier.fillMaxWidth().padding(top = defaultPadding / 2).then(
                    if (warning) Modifier.border(1.dp, MosaikStyleConfig.primaryLabelColor)
                        .padding(defaultPadding / 4)
                    else Modifier
                ),
                textAlign = TextAlign.Center,
                style = labelStyle(LabelStyle.BODY1)
            )
        }
        Text(
            remember(showReduced) {
                texts.getString(
                    if (showReduced) STRING_BUTTON_SWITCH_TO_BOXES
                    else STRING_BUTTON_SWITCH_TO_AMOUNTS
                )
            },
            Modifier.padding(top = defaultPadding / 4).clickable {
                reducedViewState.value = !reducedViewState.value
            }.padding(defaultPadding / 4).align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY2BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember(showReduced) {
                texts.getString(
                    if (showReduced) STRING_TITLE_INBOXES
                    else STRING_TITLE_INPUTS_SPENT,
                    transactionInfo.inputs.size
                )
            },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        if (showReduced)
            Text(
                remember { texts.getString(STRING_DESC_INBOXES) },
                style = labelStyle(LabelStyle.BODY2),
            )

        // Inboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.inputs.forEach { input ->
                TransactionInfoBox(
                    input.value,
                    input.address,
                    input.assets,
                    input.additionalRegisters,
                    onTokenClick,
                    texts,
                    getDb,
                )
            }
        }

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember(showReduced) {
                texts.getString(
                    if (showReduced) STRING_TITLE_OUTBOXES
                    else STRING_TITLE_OUTPUTS_CREATED,
                    transactionInfo.outputs.size
                )
            },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        if (showReduced)
            Text(
                remember { texts.getString(STRING_DESC_OUTBOXES) },
                style = labelStyle(LabelStyle.BODY2),
            )

        // Outboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.outputs.forEach { output ->
                TransactionInfoBox(
                    output.value,
                    output.address,
                    output.assets,
                    output.additionalRegisters,
                    onTokenClick,
                    texts,
                    getDb
                )
            }

        }

        AppButton(
            onConfirm,
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding)
                .widthIn(min = 120.dp)
        ) {
            Text(remember { texts.getString(STRING_LABEL_CONFIRM) })
        }
    }

}

@Composable
fun TransactionInfoLayout(
    modifier: Modifier,
    uiLogic: TransactionInfoUiLogic,
    transactionInfo: TransactionInfo,
    onTxIdClicked: () -> Unit,
    onTokenClick: ((String) -> Unit)?,
    texts: StringProvider,
    getDb: () -> IAppDatabase,
) {

    Column(modifier) {

        MiddleEllipsisText(
            transactionInfo.id,
            Modifier.fillMaxWidth().clickable { onTxIdClicked() },
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        uiLogic.transactionPurpose?.let { purpose ->
            Text(
                purpose,
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = labelStyle(LabelStyle.BODY2),
            )
        }

        Text(
            uiLogic.getTransactionExecutionState(texts),
            Modifier.fillMaxWidth().padding(top = defaultPadding),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY2BOLD),
        )

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { texts.getString(STRING_TITLE_INBOXES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        Text(
            remember { texts.getString(STRING_DESC_TRANSACTION_INBOXES) },
            style = labelStyle(LabelStyle.BODY2),
        )

        // Inboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.inputs.forEach { input ->
                TransactionInfoBox(
                    input.value,
                    input.address,
                    input.assets,
                    input.additionalRegisters,
                    onTokenClick,
                    texts,
                    getDb
                )
            }
        }

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { texts.getString(STRING_TITLE_OUTBOXES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        Text(
            remember { texts.getString(STRING_DESC_TRANSACTION_OUTBOXES) },
            style = labelStyle(LabelStyle.BODY2),
        )

        // Outboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.outputs.forEach { output ->
                TransactionInfoBox(
                    output.value,
                    output.address,
                    output.assets,
                    output.additionalRegisters,
                    onTokenClick,
                    texts,
                    getDb
                )
            }

        }
    }

}

@Composable
fun TransactionInfoBox(
    value: Long?,
    address: String,
    assets: List<AssetInstanceInfo>?,
    registerInformation: AdditionalRegisters?,
    tokenClickListener: ((String) -> Unit)?,
    texts: StringProvider,
    getDb: () -> IAppDatabase,
) {
    val addressLabelState = remember(address) { mutableStateOf<String?>(null) }
    LaunchedEffect(address) {
        withContext(Dispatchers.IO) {
            getAddressLabelFromDatabase(getDb(), address, texts)?.let {
                addressLabelState.value = it
            }
        }
    }

    Column(Modifier.padding(vertical = defaultPadding / 2)) {
        if (addressLabelState.value != null)
            Text(
                addressLabelState.value!!,
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = MosaikStyleConfig.primaryLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        else
            ErgoAddressText(
                address,
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = MosaikStyleConfig.primaryLabelColor
            )

        val nanoErgs = value ?: 0
        if (nanoErgs > 0)
            Text(
                ErgoAmount(nanoErgs).toComposableText(texts, trimTrailingZeros = true),
                Modifier.padding(horizontal = defaultPadding / 2),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )

        assets?.let {
            Column(
                Modifier.padding(horizontal = defaultPadding / 2)
                    .padding(top = if (nanoErgs > 0) defaultPadding / 2 else 0.dp)
            ) {
                assets.forEach { token ->
                    val tokenNameState = remember(token.tokenId) { mutableStateOf(token.name) }
                    if (tokenNameState.value == null)
                        LaunchedEffect(token.tokenId) {
                            withContext(Dispatchers.IO) {
                                getDb().tokenDbProvider.loadTokenInformation(token.tokenId)?.displayName?.let {
                                    tokenNameState.value = it
                                }
                            }
                        }

                    TokenEntryView(
                        TokenAmount(token.amount, token.decimals ?: 0).toStringUsFormatted(),
                        displayName = tokenNameState.value ?: token.tokenId,
                        modifier = tokenClickListener?.let {
                            Modifier.clickable {
                                tokenClickListener(
                                    token.tokenId
                                )
                            }
                        } ?: Modifier)
                }
            }
        }

        if (registerInformation?.isNotEmpty() == true) {
            Column(
                Modifier.padding(horizontal = defaultPadding / 2).padding(top = defaultPadding / 2)
            ) {
                val sortedRegisterEntries =
                    remember(registerInformation) { registerInformation.entries.sortedBy { it.key } }
                sortedRegisterEntries.forEach { entry ->
                    Text(
                        entry.key + ": " + entry.value.sigmaType + ", " + entry.value.renderedValue,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = labelStyle(LabelStyle.BODY2)
                    )
                }
            }
        }
    }

}