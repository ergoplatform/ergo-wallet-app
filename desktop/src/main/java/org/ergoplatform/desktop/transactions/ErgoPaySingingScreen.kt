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
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic

@Composable
fun ErgoPaySigningScreen(
    ergoPayState: ErgoPaySigningUiLogic.State,
    uiLogic: ErgoPaySigningUiLogic,
    onReload: () -> Unit,
    onChooseAddress: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppScrollingLayout {
        when (ergoPayState) {
            ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET -> ErgoPayChooseAddressLayout(
                uiLogic, onChooseAddress
            )
            ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> ErgoPayChooseAddressLayout(
                uiLogic, onChooseAddress
            )
            ErgoPaySigningUiLogic.State.FETCH_DATA -> {
                CircularProgressIndicator(
                    Modifier.size(48.dp).align(Alignment.Center),
                    color = uiErgoColor
                )
            }
            ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> ErgoPayTransactionInfoLayout(
                uiLogic,
                onConfirm
            )
            ErgoPaySigningUiLogic.State.DONE -> ErgoPayDoneLayout(uiLogic, onReload, onDismiss)
        }
    }
}

@Composable
private fun BoxScope.ErgoPayTransactionInfoLayout(
    uiLogic: ErgoPaySigningUiLogic,
    onConfirm: () -> Unit
) {
    uiLogic.epsr?.message?.let { message ->
        AppCard(
            Modifier.align(Alignment.Center).widthIn(max = defaultMaxWidth)
        ) {
            Row(Modifier.padding(defaultPadding)) {
                uiLogic.getDoneSeverity().getSeverityIcon()?.let { icon ->
                    Icon(
                        icon,
                        null,
                        Modifier.size(36.dp).padding(end = defaultPadding / 2)
                            .align(Alignment.CenterVertically)
                    )
                }

                Text(
                    Application.texts.getString(STRING_LABEL_MESSAGE_FROM_DAPP, message),
                    Modifier.align(Alignment.CenterVertically).weight(1f),
                    style = labelStyle(LabelStyle.BODY1),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ErgoPayChooseAddressLayout(
    uiLogic: ErgoPaySigningUiLogic,
    onChooseAddress: () -> Unit
) {
    AppCard(
        Modifier.align(Alignment.Center).widthIn(max = defaultMaxWidth)
    ) {
        Column(Modifier.padding(defaultPadding)) {
            Icon(
                ergoLogo(),
                null,
                Modifier.align(Alignment.CenterHorizontally).size(bigIconSize)
                    .padding(bottom = defaultPadding)
            )

            Text(
                Application.texts.getString(
                    when (uiLogic.state) {
                        ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET ->
                            STRING_LABEL_ERGO_PAY_CHOOSE_WALLET
                        ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS ->
                            STRING_LABEL_ERGO_PAY_CHOOSE_ADDRESS
                        else -> error("illegal state")
                    }
                ),
                textAlign = TextAlign.Center,
                style = labelStyle(LabelStyle.BODY1),
            )

            Button(
                onChooseAddress,
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                colors = primaryButtonColors(),
            ) {
                Text(
                    Application.texts.getString(
                        when (uiLogic.state) {
                            ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET -> STRING_TITLE_CHOOSE_WALLET
                            ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> STRING_TITLE_CHOOSE_ADDRESS
                            else -> error("illegal state")
                        }
                    )
                )
            }
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
