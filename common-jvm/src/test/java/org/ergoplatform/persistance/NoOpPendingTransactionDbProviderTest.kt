package org.ergoplatform.persistance

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NoOpPendingTransactionDbProviderTest {

    @Test
    fun `NoOp returns safe defaults`() = runBlocking {
        val noop = NoOpPendingTransactionDbProvider

        assertEquals(0L, noop.insertPendingTx(
            PendingTransaction(
                walletFirstAddress = "test",
                signingAddress = "test",
                submittedAtMs = 0,
                inputBoxIds = "",
                changeBoxIds = "",
                outputBoxBytes = "",
                ergDeltaNanoErg = 0,
                tokenDeltasJson = "{}"
            )
        ))

        // No-ops should not throw
        noop.updateTxId(0, "txid")
        noop.updateState(0, 1)
        noop.deletePendingTx(0)

        assertEquals(emptyList<PendingTransaction>(), noop.loadActivePendingTxs("addr"))
        assertEquals(0L, noop.sumActiveErgDeltaForAddress("addr"))
        assertEquals(0L, noop.sumActiveErgDeltaForWallet("addr"))
        assertEquals(emptyList<String>(), noop.loadActiveTokenDeltasForAddress("addr"))
        assertEquals(emptyList<String>(), noop.loadActiveTokenDeltasForWallet("addr"))
    }
}
