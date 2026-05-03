package org.ergoplatform.transactions

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.ergoplatform.persistance.PendingTransaction
import org.ergoplatform.persistance.PendingTransactionDbProvider
import org.ergoplatform.utils.LogUtils

/**
 * Stateless broadcast pipeline enforcing Insert-Broadcast-Promote atomicity.
 *
 * INVARIANTS:
 * - WAL entry is written BEFORE the network socket opens (Write-Ahead).
 * - WAL entry is NEVER deleted on broadcast failure (No-Delete).
 * - The entire sequence is wrapped in NonCancellable to survive UI teardown.
 * - DB insert failure degrades gracefully to legacy behavior (I2 compliance).
 * - txId is set deterministically at construction (FIX 1: no more null txId).
 */
object WalBroadcastPipeline {

    /**
     * Execute the atomic Insert-Broadcast-Promote sequence.
     *
     * @param pendingTx     Pre-built PendingTransaction (txId already set deterministically)
     * @param dbProvider    Room provider (or NoOp for Desktop)
     * @param broadcastFn   The actual network broadcast lambda. Returns txId on success.
     * @return txId on success, null on broadcast failure (WAL entry preserved for cleanup)
     */
    suspend fun execute(
        pendingTx: PendingTransaction,
        dbProvider: PendingTransactionDbProvider,
        broadcastFn: suspend () -> String?
    ): String? = withContext(NonCancellable) {
        // ── PHASE 1: INSERT (Write-Ahead) ──
        // FIX 2: Wrapped in try/catch to prevent SQLite DoS (disk full, DB locked).
        // If insert fails, we still broadcast (graceful degradation to legacy).
        val rowId = try {
            dbProvider.insertPendingTx(pendingTx)
        } catch (e: Throwable) {
            LogUtils.logDebug("WalBroadcastPipeline", "WAL insert failed (degrading to legacy): ${e.message}")
            -1L
        }

        // ── PHASE 2: BROADCAST ──
        val txId: String? = try {
            broadcastFn()
        } catch (_: Throwable) {
            null // Network failure → WAL stays in SUBMITTING
        }

        // ── PHASE 3: PROMOTE or PRESERVE ──
        if (txId != null && rowId > 0) {
            // Success + WAL intact: promote to SUBMITTED
            // txId is already set deterministically — just update state
            try {
                dbProvider.updateState(rowId, PendingTransaction.STATE_SUBMITTED)
            } catch (_: Throwable) {
                // State update failure is non-fatal — reconciliation handles it
            }
        }
        // On failure: DO NOT delete. DO NOT update state.
        // Reconciliation will purge via tri-state Oracle.

        txId
    }
}
