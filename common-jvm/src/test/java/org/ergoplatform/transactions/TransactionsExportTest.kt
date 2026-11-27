package org.ergoplatform.transactions

import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.persistance.AddressTransaction
import org.ergoplatform.persistance.AddressTransactionToken
import org.ergoplatform.persistance.TX_STATE_CONFIRMED_SECURE
import org.ergoplatform.persistance.TX_STATE_WAITING
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsExportTest {

    @Test
    fun testFormatCsvTextField() {
        assertEquals("\"\"", TransactionsExport.formatCsvTextField(null))
        assertEquals("\"\"", TransactionsExport.formatCsvTextField(""))
        assertEquals("\"Plain note\"", TransactionsExport.formatCsvTextField("Plain note"))
        assertEquals("\"part1;part2\"", TransactionsExport.formatCsvTextField("part1;part2"))
        assertEquals("\"say \"\"hello\"\"\"", TransactionsExport.formatCsvTextField("say \"hello\""))
        assertEquals(
            "\"line1 line2 line3 line4\"",
            TransactionsExport.formatCsvTextField("line1\nline2\rline3\r\nline4")
        )
        assertEquals("\"'=SUM(A1:A2)\"", TransactionsExport.formatCsvTextField("=SUM(A1:A2)"))
        assertEquals("\"'+1+2\"", TransactionsExport.formatCsvTextField("+1+2"))
        assertEquals("\"'-100\"", TransactionsExport.formatCsvTextField("-100"))
        assertEquals("\"'@SUM(A1)\"", TransactionsExport.formatCsvTextField("@SUM(A1)"))
        assertEquals("\"'\t1+1\"", TransactionsExport.formatCsvTextField("\t1+1"))
        assertEquals(
            "\"'=a;\"\"b\"\" c\"",
            TransactionsExport.formatCsvTextField("=a;\"b\"\nc")
        )
    }

    @Test
    fun testExportTransactionsEscapesTokenNameAndMessage() {
        val timestamp = 1_700_000_000_000L
        val expectedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US).format(Date(timestamp))

        val confirmedTx = AddressTransactionWithTokens(
            addressTransaction = AddressTransaction(
                id = 1,
                address = "9fAddress",
                txId = "tx123",
                inclusionHeight = 1000L,
                timestamp = timestamp,
                ergAmount = ErgoAmount(1_500_000_000L),
                message = "=SUM(A1);\"quoted\"\r\nnext",
                state = TX_STATE_CONFIRMED_SECURE,
            ),
            tokens = listOf(
                AddressTransactionToken(
                    id = 1,
                    address = "9fAddress",
                    txId = "tx123",
                    tokenId = "tok1",
                    name = "Tok;\"en\"\nName",
                    tokenAmount = TokenAmount(250L, 2),
                ),
                AddressTransactionToken(
                    id = 2,
                    address = "9fAddress",
                    txId = "tx123",
                    tokenId = "tok2",
                    name = "+Bonus",
                    tokenAmount = TokenAmount(-5L, 0),
                ),
            ),
        )

        val unconfirmedTx = AddressTransactionWithTokens(
            addressTransaction = AddressTransaction(
                id = 2,
                address = "9fAddress",
                txId = "txUnconfirmed",
                inclusionHeight = 1001L,
                timestamp = timestamp,
                ergAmount = ErgoAmount(1_000_000_000L),
                message = "pending",
                state = TX_STATE_WAITING,
            ),
            tokens = emptyList(),
        )

        val stringWriter = StringWriter()
        TransactionsExport.exportTransactions(listOf(confirmedTx, unconfirmedTx)) {
            BufferedWriter(stringWriter)
        }

        val lines = stringWriter.toString().split('\n').dropLastWhile { it.isEmpty() }
        assertEquals(4, lines.size)
        assertEquals("Address;txid;block;timestamp;value;token;tokenid;message", lines[0])
        assertEquals(
            "9fAddress;tx123;1000;$expectedDate;1.5;ERG;;\"'=SUM(A1);\"\"quoted\"\" next\"",
            lines[1]
        )
        assertEquals(
            "9fAddress;tx123;1000;$expectedDate;2.5;\"Tok;\"\"en\"\" Name\";tok1;\"'=SUM(A1);\"\"quoted\"\" next\"",
            lines[2]
        )
        assertEquals(
            "9fAddress;tx123;1000;$expectedDate;-5;\"'+Bonus\";tok2;\"'=SUM(A1);\"\"quoted\"\" next\"",
            lines[3]
        )
    }
}
