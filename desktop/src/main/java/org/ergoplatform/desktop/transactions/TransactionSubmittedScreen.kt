package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_DONE
import org.ergoplatform.uilogic.STRING_DESC_TRANSACTION_SEND

@Composable
fun TransactionSubmittedScreen(
    txId: String,
    onDismiss: () -> Unit,
) {
    AppScrollingLayout {
        Card(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {
                Icon(
                    Icons.Default.AddTask,
                    null,
                    Modifier.size(75.dp).align(Alignment.CenterHorizontally),
                    uiErgoColor
                )

                Text(
                    remember { Application.texts.getString(STRING_DESC_TRANSACTION_SEND) },
                    Modifier.fillMaxWidth().padding(top = defaultPadding),
                    textAlign = TextAlign.Center,
                    style = labelStyle(LabelStyle.BODY1)
                )

                Row(Modifier.padding(top = defaultPadding)) {
                    Text(
                        text = txId,
                        style = labelStyle(LabelStyle.BODY1BOLD),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        onClick = {
                            openBrowser(getExplorerTxUrl(txId))
                        },
                        Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.Share, null)
                    }
                }

                Button(
                    onDismiss,
                    Modifier.padding(top = defaultPadding).align(Alignment.CenterHorizontally),
                    colors = primaryButtonColors()
                ) {
                    Text(remember { Application.texts.getString(STRING_BUTTON_DONE) })
                }
            }
        }
    }
}