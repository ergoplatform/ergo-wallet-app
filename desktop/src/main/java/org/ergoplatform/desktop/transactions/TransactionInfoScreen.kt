package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.compose.transactions.TransactionInfoLayout
import org.ergoplatform.desktop.ui.AppScrollingLayout
import org.ergoplatform.desktop.ui.defaultMaxWidth
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.STRING_LABEL_ERROR_FETCHING
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic

@Composable
fun TransactionInfoScreen(
    ti: TransactionInfo?,
    loading: Boolean,
    uiLogic: TransactionInfoUiLogic,
    onTxIdClicked: () -> Unit,
    onTokenClick: (String) -> Unit,
) {
    AppScrollingLayout {
        if (loading) {
            AppProgressIndicator()
        } else if (ti == null) {
            Text(
                remember { Application.texts.getString(STRING_LABEL_ERROR_FETCHING) },
                Modifier.align(Alignment.Center).padding(horizontal = defaultPadding)
            )
        } else {

            Card(
                Modifier.padding(defaultPadding).align(Alignment.Center)
                    .defaultMinSize(400.dp, 200.dp)
                    .widthIn(max = defaultMaxWidth)
            ) {
                TransactionInfoLayout(
                    Modifier.padding(defaultPadding),
                    uiLogic,
                    ti,
                    onTxIdClicked,
                    onTokenClick,
                    Application.texts,
                    getDb = { Application.database }
                )
            }
        }
    }
}