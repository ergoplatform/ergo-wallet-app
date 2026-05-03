package org.ergoplatform.persistance

/**
 * No-op implementation for platforms without Room (Desktop, iOS).
 * WAL features are silently disabled — the wallet falls back to
 * legacy behavior (no local mempool tracking).
 *
 * This is safe because:
 * - LocalAwareUnspentBoxesLoader with emptyList() = RecordingBoxesLoader behavior
 * - UI delta calculations return 0 → no balance adjustment
 * - SendFundsUiLogic try/catch returns emptyList() on failure anyway
 */
object NoOpPendingTransactionDbProvider : PendingTransactionDbProvider {
    override suspend fun insertPendingTx(tx: PendingTransaction): Long = 0L
    override suspend fun updateTxId(rowId: Long, txId: String) {}
    override suspend fun updateState(rowId: Long, state: Int) {}
    override suspend fun renewSubmittedAt(rowId: Long, newTimestamp: Long) {}
    override suspend fun deletePendingTx(rowId: Long) {}
    override suspend fun loadActivePendingTxs(walletFirstAddress: String): List<PendingTransaction> = emptyList()
    override suspend fun sumActiveErgDeltaForAddress(signingAddress: String): Long = 0L
    override suspend fun sumActiveErgDeltaForWallet(walletFirstAddress: String): Long = 0L
    override suspend fun loadActiveTokenDeltasForAddress(signingAddress: String): List<String> = emptyList()
    override suspend fun loadActiveTokenDeltasForWallet(walletFirstAddress: String): List<String> = emptyList()
}
