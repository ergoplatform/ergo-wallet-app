package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ergoplatform.Application
import org.ergoplatform.compose.transactions.SignTransactionInfoLayout
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.transactions.TransactionInfo

@Composable
fun ConfirmSendFundsDialog(
    transactionInfo: TransactionInfo,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(onDismissRequest) {
        val scrollState = rememberScrollState()
        Box(Modifier.verticalScroll(scrollState)) {

            SignTransactionInfoLayout(
                Modifier.padding(defaultPadding),
                transactionInfo,
                onConfirm,
                onTokenClick = null,
                Application.texts,
                getDb = { Application.database }
            )

        }
    }
}