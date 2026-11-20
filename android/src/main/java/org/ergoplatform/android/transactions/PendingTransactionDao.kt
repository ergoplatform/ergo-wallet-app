package org.ergoplatform.android.transactions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingTransactionDao {
    @Insert
    suspend fun insert(entity: PendingTransactionDbEntity): Long

    @Query("UPDATE pending_transactions SET tx_id = :txId WHERE id = :id")
    suspend fun updateTxId(id: Long, txId: String)

    @Query("UPDATE pending_transactions SET state = :state WHERE id = :id")
    suspend fun updateState(id: Long, state: Int)

    @Query("UPDATE pending_transactions SET submitted_at_ms = :newTimestamp WHERE id = :id")
    suspend fun renewSubmittedAt(id: Long, newTimestamp: Long)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM pending_transactions WHERE wallet_first_address = :walletFirstAddress AND state < 2 ORDER BY submitted_at_ms DESC")
    suspend fun loadActivePendingTxs(walletFirstAddress: String): List<PendingTransactionDbEntity>

    @Query("SELECT COALESCE(SUM(erg_delta_nanoerg), 0) FROM pending_transactions WHERE signing_address = :address AND state < 2")
    suspend fun sumActiveErgDeltaForAddress(address: String): Long

    @Query("SELECT COALESCE(SUM(erg_delta_nanoerg), 0) FROM pending_transactions WHERE wallet_first_address = :walletFirstAddress AND state < 2")
    suspend fun sumActiveErgDeltaForWallet(walletFirstAddress: String): Long

    @Query("SELECT token_deltas_json FROM pending_transactions WHERE signing_address = :address AND state < 2")
    suspend fun loadActiveTokenDeltasForAddress(address: String): List<String>

    @Query("SELECT token_deltas_json FROM pending_transactions WHERE wallet_first_address = :walletFirstAddress AND state < 2")
    suspend fun loadActiveTokenDeltasForWallet(walletFirstAddress: String): List<String>
}
