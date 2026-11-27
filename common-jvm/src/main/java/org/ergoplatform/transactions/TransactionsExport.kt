package org.ergoplatform.transactions

import org.ergoplatform.persistance.TX_STATE_CONFIRMED_UNSECURE
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import java.io.BufferedWriter
import java.text.SimpleDateFormat
import java.util.*

object TransactionsExport {
    fun exportTransactions(transactions: List<AddressTransactionWithTokens>, getWriter: () -> BufferedWriter) {
        val filteredTx =
            transactions.filter { it.addressTransaction.state >= TX_STATE_CONFIRMED_UNSECURE }

        if (filteredTx.isEmpty())
            throw IllegalArgumentException("No confirmed transactions to export.")

        getWriter().use { out ->

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US)

            out.write("Address;txid;block;timestamp;value;token;tokenid;message\n")

            filteredTx.forEach {
                val ergValue = it.addressTransaction
                val tokens = it.tokens
                val dateFormatted = sdf.format(Date(ergValue.timestamp))
                val messageFormatted = formatCsvTextField(ergValue.message)

                if (!ergValue.ergAmount.isZero())
                    out.write(
                        "${ergValue.address};${ergValue.txId};${ergValue.inclusionHeight};"
                                + "$dateFormatted;${ergValue.ergAmount.toStringTrimTrailingZeros()};ERG;;"
                                + "$messageFormatted\n"
                    )

                tokens.forEach { token ->
                    val tokenNameFormatted = formatCsvTextField(token.name)
                    out.write(
                        "${token.address};${token.txId};${ergValue.inclusionHeight};"
                                + "$dateFormatted;${token.tokenAmount.toStringTrimTrailingZeros()};$tokenNameFormatted;"
                                + "${token.tokenId};$messageFormatted\n"
                    )
                }
            }
        }
    }

    // Quote and escape CSV fields containing delimiters, quotes, newlines, or leading formula characters
    internal fun formatCsvTextField(value: String?): String {
        val singleLine = (value ?: "")
            .replace("\r\n", " ")
            .replace('\r', ' ')
            .replace('\n', ' ')
        val prefixed = when (singleLine.firstOrNull()) {
            '=', '+', '-', '@', '\t' -> "'$singleLine"
            else -> singleLine
        }
        return "\"${prefixed.replace("\"", "\"\"")}\""
    }
}