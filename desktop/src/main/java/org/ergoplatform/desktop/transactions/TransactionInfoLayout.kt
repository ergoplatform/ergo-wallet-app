package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.desktop.tokens.TokenEntryView
import org.ergoplatform.desktop.ui.ErgoAddressText
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.toComposableText
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic

@Composable
fun SignTransactionInfoLayout(
    modifier: Modifier,
    transactionInfo: TransactionInfo,
    onConfirm: () -> Unit,
    onTokenClick: ((String) -> Unit)?
) {

    Column(modifier) {

        Text(
            remember { Application.texts.getString(STRING_DESC_SIGNING_REQUEST) },
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY2BOLD),
        )

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { Application.texts.getString(STRING_TITLE_INBOXES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = uiErgoColor,
        )

        Text(
            remember { Application.texts.getString(STRING_DESC_INBOXES) },
            style = labelStyle(LabelStyle.BODY2),
        )

        // Inboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.inputs.forEach { input ->
                TransactionInfoBox(
                    input.value,
                    input.address,
                    input.assets,
                    onTokenClick,
                )
            }
        }

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { Application.texts.getString(STRING_TITLE_OUTBOXES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = uiErgoColor,
        )

        Text(
            remember { Application.texts.getString(STRING_DESC_OUTBOXES) },
            style = labelStyle(LabelStyle.BODY2),
        )

        // Outboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.outputs.forEach { output ->
                TransactionInfoBox(
                    output.value,
                    output.address,
                    output.assets,
                    onTokenClick,
                )
            }

        }

        Button(
            onConfirm,
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding)
                .widthIn(min = 120.dp)
        ) {
            Text(remember { Application.texts.getString(STRING_LABEL_CONFIRM) })
        }
    }

}

@Composable
fun TransactionInfoLayout(
    modifier: Modifier,
    uiLogic: TransactionInfoUiLogic,
    transactionInfo: TransactionInfo,
    onTxIdClicked: () -> Unit,
    onTokenClick: ((String) -> Unit)?
) {

    Column(modifier) {

        MiddleEllipsisText(
            transactionInfo.id,
            Modifier.fillMaxWidth().clickable { onTxIdClicked() },
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = uiErgoColor,
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
            uiLogic.getTransactionExecutionState(Application.texts),
            Modifier.fillMaxWidth().padding(top = defaultPadding),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY2BOLD),
        )

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { Application.texts.getString(STRING_TITLE_INBOXES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = uiErgoColor,
        )

        Text(
            remember { Application.texts.getString(STRING_DESC_TRANSACTION_INBOXES) },
            style = labelStyle(LabelStyle.BODY2),
        )

        // Inboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.inputs.forEach { input ->
                TransactionInfoBox(
                    input.value,
                    input.address,
                    input.assets,
                    onTokenClick,
                )
            }
        }

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { Application.texts.getString(STRING_TITLE_OUTBOXES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = uiErgoColor,
        )

        Text(
            remember { Application.texts.getString(STRING_DESC_TRANSACTION_OUTBOXES) },
            style = labelStyle(LabelStyle.BODY2),
        )

        // Outboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            transactionInfo.outputs.forEach { output ->
                TransactionInfoBox(
                    output.value,
                    output.address,
                    output.assets,
                    onTokenClick,
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
    tokenClickListener: ((String) -> Unit)?,
) {
    val addressLabelState = remember(address) { mutableStateOf<String?>(null) }
    LaunchedEffect(address) {
        getAddressLabelFromDatabase(Application.database, address, Application.texts)?.let {
            addressLabelState.value = it
        }
    }

    Column(Modifier.padding(vertical = defaultPadding / 2)) {
        if (addressLabelState.value != null)
            Text(
                addressLabelState.value!!,
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = uiErgoColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        else
            ErgoAddressText(address, style = labelStyle(LabelStyle.BODY1BOLD), color = uiErgoColor)

        val nanoErgs = value ?: 0
        if (nanoErgs > 0)
            Text(
                ErgoAmount(nanoErgs).toComposableText(),
                Modifier.padding(horizontal = defaultPadding / 2),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )

        assets?.let {
            Column(
                Modifier.padding(horizontal = defaultPadding / 2).padding(top = defaultPadding / 2)
            ) {
                assets.forEach { token ->
                    TokenEntryView(
                        TokenAmount(token.amount, token.decimals ?: 0).toStringUsFormatted(),
                        displayName = token.name ?: token.tokenId,
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
    }

}