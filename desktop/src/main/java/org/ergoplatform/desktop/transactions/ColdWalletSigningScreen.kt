package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.coldSigningResponseToQrChunks
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.ColdWalletSigningUiLogic
import org.ergoplatform.utils.LogUtils

@Composable
fun ColdWalletSigningScreen(
    uiLogic: ColdWalletSigningUiLogic,
    scanningState: MutableState<Int>,
    txInfoState: MutableState<TransactionInfo?>,
    onScan: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // needed to enforce a refresh
    LogUtils.logDebug("ColdWalletSigningScreen", "State ${scanningState.value}")

    AppScrollingLayout {
        val modifier =
            Modifier.padding(defaultPadding).widthIn(max = defaultMaxWidth).align(Alignment.Center)

        when (uiLogic.state) {
            ColdWalletSigningUiLogic.State.SCANNING -> {
                if (uiLogic.qrPagesCollector.pagesAdded < uiLogic.qrPagesCollector.pagesCount)
                    ColdSigningScanLayout(
                        uiLogic,
                        modifier,
                        scanningState,
                        onScan
                    )
                else
                    AppProgressIndicator() // after scanning, we have a second until data is prepared
            }

            ColdWalletSigningUiLogic.State.WAITING_TO_CONFIRM -> ColdSigningTransactionInfoLayout(
                txInfoState,
                modifier,
                onConfirm
            )

            ColdWalletSigningUiLogic.State.PRESENT_RESULT -> ColdSigningResultLayout(
                uiLogic, modifier, onDismiss
            )
        }
    }
}

@Composable
fun ColdSigningResultLayout(
    uiLogic: ColdWalletSigningUiLogic,
    modifier: Modifier,
    onDismiss: () -> Unit
) {
    var lowRes by remember { mutableStateOf(false) }

    AppCard(modifier.fillMaxWidth()) {
        Box(Modifier.padding(defaultPadding)) {
            PagedQrContainer(
                lowRes,
                calcChunks = { limit ->
                    coldSigningResponseToQrChunks(uiLogic.signedQrCode!!, limit)
                },
                onDismiss,
                lastPageButtonLabel = STRING_LABEL_DISMISS,
                descriptionLabel = STRING_DESC_SHOW_SIGNED_MULTIPLE,
                lastPageDescriptionLabel = STRING_DESC_SHOW_SIGNED,
                modifier = Modifier.padding(top = defaultPadding * 1.5f)
            )

            // TODO cold wallet save/load functionality
            IconButton({ lowRes = !lowRes }, Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.BurstMode, null)
            }
        }
    }
}

@Composable
private fun ColdSigningScanLayout(
    uiLogic: ColdWalletSigningUiLogic,
    modifier: Modifier,
    scanningState: MutableState<Int>,
    onScan: () -> Unit,
) {

    AppCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(defaultPadding)) {
            Icon(
                Icons.Default.QrCode,
                null,
                Modifier.size(200.dp).align(Alignment.CenterHorizontally)
                    .padding(vertical = defaultPadding)
            )

            Text(
                remember(scanningState.value) {
                    Application.texts.getString(
                        STRING_LABEL_QR_PAGES_INFO,
                        uiLogic.qrPagesCollector.pagesAdded,
                        uiLogic.qrPagesCollector.pagesCount
                    )
                },
                Modifier.align(Alignment.CenterHorizontally)
                    .padding(horizontal = defaultPadding),
                style = labelStyle(LabelStyle.HEADLINE2),
            )

            uiLogic.lastErrorMessage?.let { errorMsg ->
                Text(
                    errorMsg,
                    Modifier.fillMaxWidth().padding(horizontal = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onScan,
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                colors = primaryButtonColors(),
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    null,
                    Modifier.padding(end = defaultPadding / 2)
                )
                Text(remember { Application.texts.getString(STRING_LABEL_SCAN_QR) })

            }
        }
    }

}

@Composable
private fun ColdSigningTransactionInfoLayout(
    txInfoState: MutableState<TransactionInfo?>,
    modifier: Modifier,
    onConfirm: () -> Unit
) {
    AppCard(modifier.fillMaxWidth()) {
        TransactionInfoLayout(
            Modifier.padding(defaultPadding),
            txInfoState.value!!,
            onConfirm = onConfirm,
            onTokenClick = null
        )
    }
}
