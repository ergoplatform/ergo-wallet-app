package org.ergoplatform.persistance

/**
 * Platform-agnostic interface for pending transaction persistence.
 *
 * Implemented by:
 * - PendingTransactionDbProviderImpl (Android Room)
 * - NoOpPendingTransactionDbProvider (Desktop/iOS fallback)
 */
interface PendingTransactionDbProvider {
    suspend fun insertPendingTx(tx: PendingTransaction): Long
    suspend fun updateTxId(rowId: Long, txId: String)
    suspend fun updateState(rowId: Long, state: Int)
    suspend fun renewSubmittedAt(rowId: Long, newTimestamp: Long)
    suspend fun deletePendingTx(rowId: Long)
    suspend fun loadActivePendingTxs(walletFirstAddress: String): List<PendingTransaction>
    suspend fun sumActiveErgDeltaForAddress(signingAddress: String): Long
    suspend fun sumActiveErgDeltaForWallet(walletFirstAddress: String): Long
    suspend fun loadActiveTokenDeltasForAddress(signingAddress: String): List<String>
    suspend fun loadActiveTokenDeltasForWallet(walletFirstAddress: String): List<String>
}
