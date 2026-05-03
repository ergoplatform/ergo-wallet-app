package org.ergoplatform.android.transactions

import org.ergoplatform.android.AppDatabase
import org.ergoplatform.persistance.PendingTransaction
import org.ergoplatform.persistance.PendingTransactionDbProvider

class RoomPendingTransactionDbProvider(
    private val database: AppDatabase
) : PendingTransactionDbProvider {

    override suspend fun insertPendingTx(pendingTx: PendingTransaction): Long {
        return database.pendingTransactionDao().insert(pendingTx.toDbEntity())
    }

    override suspend fun updateTxId(id: Long, txId: String) {
        database.pendingTransactionDao().updateTxId(id, txId)
    }

    override suspend fun updateState(id: Long, state: Int) {
        database.pendingTransactionDao().updateState(id, state)
    }

    override suspend fun renewSubmittedAt(id: Long, newTimestamp: Long) {
        database.pendingTransactionDao().renewSubmittedAt(id, newTimestamp)
    }

    override suspend fun deletePendingTx(id: Long) {
        database.pendingTransactionDao().delete(id)
    }

    override suspend fun loadActivePendingTxs(walletFirstAddress: String): List<PendingTransaction> {
        return database.pendingTransactionDao()
            .loadActivePendingTxs(walletFirstAddress)
            .map { it.toModel() }
    }

    override suspend fun sumActiveErgDeltaForAddress(address: String): Long {
        return database.pendingTransactionDao().sumActiveErgDeltaForAddress(address)
    }

    override suspend fun sumActiveErgDeltaForWallet(walletFirstAddress: String): Long {
        return database.pendingTransactionDao().sumActiveErgDeltaForWallet(walletFirstAddress)
    }

    override suspend fun loadActiveTokenDeltasForAddress(address: String): List<String> {
        return database.pendingTransactionDao().loadActiveTokenDeltasForAddress(address)
    }

    override suspend fun loadActiveTokenDeltasForWallet(walletFirstAddress: String): List<String> {
        return database.pendingTransactionDao().loadActiveTokenDeltasForWallet(walletFirstAddress)
    }
}
