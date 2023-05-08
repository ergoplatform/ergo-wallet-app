package org.ergoplatform.transactions

import org.ergoplatform.persistance.TX_STATE_CONFIRMED_UNSECURE
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TransactionsExport {
    fun exportTransactions(transactions: List<AddressTransactionWithTokens>, file: File) {
        val filteredTx =
            transactions.filter { it.addressTransaction.state >= TX_STATE_CONFIRMED_UNSECURE }

        if (filteredTx.isEmpty())
            throw IllegalArgumentException("No confirmed transactions to export.")

        file.bufferedWriter().use { out ->

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US)

            out.write("Address;txid;block;timestamp;value;token;tokenid;message\n")

            filteredTx.forEach {
                val ergValue = it.addressTransaction
                val tokens = it.tokens
                val dateFormatted = sdf.format(Date(ergValue.timestamp))
                val messageEscaped = ergValue.message?.replace("\"", "\"\"") ?: ""

                if (!ergValue.ergAmount.isZero())
                    out.write(
                        "${ergValue.address};${ergValue.txId};${ergValue.inclusionHeight};"
                                + "$dateFormatted;${ergValue.ergAmount.toStringTrimTrailingZeros()};ERG;;"
                                + "\"$messageEscaped\"\n"
                    )

                tokens.forEach { token ->
                    out.write(
                        "${token.address};${token.txId};${ergValue.inclusionHeight};"
                                + "$dateFormatted;${token.tokenAmount.toStringTrimTrailingZeros()};${token.name};"
                                + "${token.tokenId};\"$messageEscaped\"\n"
                    )
                }
            }
        }
    }
}