package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.*

@Composable
fun TransactionInfoLayout(
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
        Column(Modifier.padding(defaultPadding / 2)) { }

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
        Column(Modifier.padding(defaultPadding / 2)) { }

        Button(
            onConfirm,
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding)
                .widthIn(min = 120.dp)
        ) {
            Text(remember { Application.texts.getString(STRING_LABEL_CONFIRM) })
        }
    }

}