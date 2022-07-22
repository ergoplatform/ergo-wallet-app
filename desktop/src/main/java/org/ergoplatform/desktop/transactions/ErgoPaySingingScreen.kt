package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.STRING_BUTTON_RETRY
import org.ergoplatform.uilogic.STRING_LABEL_DISMISS
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic

@Composable
fun ErgoPaySigningScreen(
    ergoPayState: ErgoPaySigningUiLogic.State,
    uiLogic: ErgoPaySigningUiLogic,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppScrollingLayout {
        when (ergoPayState) {
            ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET -> TODO()
            ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> TODO()
            ErgoPaySigningUiLogic.State.FETCH_DATA -> {
                CircularProgressIndicator(
                    Modifier.size(48.dp).align(Alignment.Center),
                    color = uiErgoColor
                )
            }
            ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> TODO()
            ErgoPaySigningUiLogic.State.DONE -> ErgoPayDoneLayout(uiLogic, onReload, onDismiss)
        }
    }
}

@Composable
private fun BoxScope.ErgoPayDoneLayout(
    uiLogic: ErgoPaySigningUiLogic,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppCard(
        Modifier.align(Alignment.Center).widthIn(max = defaultMaxWidth)
    ) {
        Column(Modifier.padding(defaultPadding)) {
            uiLogic.getDoneSeverity().getSeverityIcon()?.let { icon ->
                Icon(
                    icon,
                    null,
                    Modifier.align(Alignment.CenterHorizontally).size(96.dp)
                        .padding(bottom = defaultPadding)
                )
            }

            Text(
                uiLogic.getDoneMessage(Application.texts),
                textAlign = TextAlign.Center,
                style = labelStyle(LabelStyle.BODY1),
            )

            val dismissShouldRetry =
                uiLogic.getDoneSeverity() == MessageSeverity.ERROR && uiLogic.canReloadFromDapp()
            Button(
                {
                    if (dismissShouldRetry) {
                        onReload()
                    } else {
                        onDismiss()
                    }
                },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                colors = primaryButtonColors(),
            ) {
                Text(
                    Application.texts.getString(
                        if (dismissShouldRetry) STRING_BUTTON_RETRY
                        else STRING_LABEL_DISMISS
                    )
                )
            }
        }
    }
}
