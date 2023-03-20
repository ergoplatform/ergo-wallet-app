package org.ergoplatform.compose.multisig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.mediumIconSize
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.MultisigTransaction
import org.ergoplatform.persistance.TransactionDbProvider
import org.ergoplatform.uilogic.STRING_MULTISIG_NO_MEMO
import org.ergoplatform.uilogic.STRING_TITLE_PENDING_MULTISIG_TX
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.transactions.getTransactionStateString
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic
import org.ergoplatform.utils.millisecondsToLocalTime
import org.ergoplatform.wallet.isMultisig

@Composable
fun MultisigTransactionsListLayout(
    informationVersion: Int,
    downloadingTransactions: Boolean,
    uiLogic: WalletDetailsUiLogic,
    onMultisigTransactionClicked: (Int) -> Unit,
    texts: StringProvider,
    getDb: () -> TransactionDbProvider,
) {
    if (uiLogic.wallet?.isMultisig() == true) {
        val transactionList =
            remember { mutableStateOf(emptyList<MultisigTransaction>()) }
        LaunchedEffect(informationVersion, downloadingTransactions) {
            transactionList.value = uiLogic.loadMultisigTransactions(getDb())
        }

        if (transactionList.value.isNotEmpty())
            AppCard(Modifier.padding(defaultPadding)) {
                Column {

                    // HEADER
                    Row(Modifier.padding(defaultPadding)) {
                        Icon(
                            Icons.Default.PeopleAlt, null,
                            Modifier.requiredSize(mediumIconSize)
                        )

                        Text(
                            remember { texts.getString(STRING_TITLE_PENDING_MULTISIG_TX) },
                            Modifier.padding(start = defaultPadding)
                                .align(Alignment.CenterVertically).weight(1f),
                            style = labelStyle(LabelStyle.BODY1BOLD),
                            color = MosaikStyleConfig.primaryLabelColor,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }

                    transactionList.value.forEach { transaction ->
                        Divider()

                        key(transaction) {
                            MultisigTransactionInfo(
                                transaction,
                                Modifier.clickable { onMultisigTransactionClicked(transaction.id) },
                                texts,
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun MultisigTransactionInfo(
    transaction: MultisigTransaction,
    modifier: Modifier,
    texts: StringProvider,
) {
    Column(modifier.padding(defaultPadding)) {

        Row {
            Text(
                transaction.getTransactionStateString(texts),
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = MosaikStyleConfig.primaryLabelColor,
            )

            Text(
                millisecondsToLocalTime(transaction.lastChange),
                Modifier.padding(start = defaultPadding).weight(1f),
                textAlign = TextAlign.End,
            )
        }

        Text(
            transaction.memo ?: texts.getString(STRING_MULTISIG_NO_MEMO),
            Modifier.padding(top = defaultPadding / 2),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

    }
}
