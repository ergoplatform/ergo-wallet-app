package org.ergoplatform.transactions

import kotlinx.coroutines.runBlocking
import org.ergoplatform.persistance.PendingTransaction
import org.ergoplatform.persistance.PendingTransactionDbProvider
import org.junit.Assert.*
import org.junit.Test

class WalBroadcastPipelineTest {

    // ══════════════════════════════════════════════════════════════
    // Fake DB Provider (spy pattern)
    // ══════════════════════════════════════════════════════════════

    private class FakeDbProvider : PendingTransactionDbProvider {
        var insertCalled = false
        var insertShouldThrow = false
        var updateStateCalled = false
        var lastStateUpdate: Int? = null
        var lastRowId: Long? = null

        override suspend fun insertPendingTx(tx: PendingTransaction): Long {
            insertCalled = true
            if (insertShouldThrow) throw RuntimeException("SQLite: disk full")
            return 42L
        }

        override suspend fun updateTxId(rowId: Long, txId: String) {}
        override suspend fun updateState(rowId: Long, state: Int) {
            updateStateCalled = true
            lastRowId = rowId
            lastStateUpdate = state
        }
        override suspend fun renewSubmittedAt(rowId: Long, newTimestamp: Long) {}
        override suspend fun deletePendingTx(rowId: Long) {}
        override suspend fun loadActivePendingTxs(walletFirstAddress: String) = emptyList<PendingTransaction>()
        override suspend fun sumActiveErgDeltaForAddress(signingAddress: String) = 0L
        override suspend fun sumActiveErgDeltaForWallet(walletFirstAddress: String) = 0L
        override suspend fun loadActiveTokenDeltasForAddress(signingAddress: String) = emptyList<String>()
        override suspend fun loadActiveTokenDeltasForWallet(walletFirstAddress: String) = emptyList<String>()
    }

    private fun makePendingTx() = PendingTransaction(
        walletFirstAddress = "9test",
        signingAddress = "9test",
        submittedAtMs = System.currentTimeMillis(),
        txId = "abc123def456",
        inputBoxIds = "box1,box2",
        changeBoxIds = "",
        outputBoxBytes = "",
        ergDeltaNanoErg = -1000000L,
        tokenDeltasJson = "{}"
    )

    // ══════════════════════════════════════════════════════════════
    // Test A: Happy Path (Insert -> Broadcast -> Promote)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `happy path - insert broadcast promote`() = runBlocking {
        val db = FakeDbProvider()
        val txId = WalBroadcastPipeline.execute(
            pendingTx = makePendingTx(),
            dbProvider = db,
            broadcastFn = { "txid_from_network" }
        )

        assertEquals("txid_from_network", txId)
        assertTrue("Insert must be called", db.insertCalled)
        assertTrue("State must be promoted", db.updateStateCalled)
        assertEquals(PendingTransaction.STATE_SUBMITTED, db.lastStateUpdate)
        assertEquals(42L, db.lastRowId)
    }

    // ══════════════════════════════════════════════════════════════
    // Test B: SQLite DoS (I2 compliance - graceful degradation)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `sqlite failure still broadcasts - I2 compliance`() = runBlocking {
        val db = FakeDbProvider()
        db.insertShouldThrow = true

        var broadcastCalled = false
        val txId = WalBroadcastPipeline.execute(
            pendingTx = makePendingTx(),
            dbProvider = db,
            broadcastFn = {
                broadcastCalled = true
                "txid_despite_db_failure"
            }
        )

        assertEquals("txid_despite_db_failure", txId)
        assertTrue("Insert must be attempted", db.insertCalled)
        assertTrue("Broadcast MUST proceed despite DB failure", broadcastCalled)
        assertFalse("State must NOT be updated (rowId = -1)", db.updateStateCalled)
    }

    // ══════════════════════════════════════════════════════════════
    // Test C: Network Failure (WAL preserved, no state update)
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `network failure preserves WAL entry`() = runBlocking {
        val db = FakeDbProvider()
        val txId = WalBroadcastPipeline.execute(
            pendingTx = makePendingTx(),
            dbProvider = db,
            broadcastFn = { throw RuntimeException("Connection refused") }
        )

        assertNull("txId must be null on network failure", txId)
        assertTrue("Insert must be called", db.insertCalled)
        assertFalse("State must NOT be updated on failure", db.updateStateCalled)
    }
}
