package org.ergoplatform.android.transactions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.ergoplatform.android.multisig.MultisigTransactionDbEntity

@Dao
interface TransactionDbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAddressTransaction(vararg addressTransactions: AddressTransactionDbEntity)

    @Query("SELECT * FROM address_transaction WHERE address = :address LIMIT :limit OFFSET :limit * :page") // ORDER BY inclusion_height DESC done by index
    suspend fun loadAddressTransactions(
        address: String,
        limit: Int,
        page: Int
    ): List<AddressTransactionDbEntity>

    @Query("SELECT * FROM address_transaction WHERE id = :id")
    suspend fun loadAddressTransaction(id: Int): AddressTransactionDbEntity?

    @Query("SELECT * FROM address_transaction WHERE address = :address AND tx_id = :txId")
    suspend fun loadAddressTransaction(address: String, txId: String): AddressTransactionDbEntity?

    @Query("DELETE FROM address_transaction WHERE id = :id")
    suspend fun deleteAddressTransaction(id: Int)

    @Query("DELETE FROM address_transaction WHERE address = :address")
    suspend fun deleteAddressTransactions(address: String)

    @Query("DELETE FROM address_transaction_token WHERE address = :address")
    suspend fun deleteAddressTransactionTokens(address: String)

    @Query("DELETE FROM address_transaction_token WHERE address = :address AND tx_id = :txId")
    suspend fun deleteAddressTransactionTokens(address: String, txId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAddressTransactionToken(vararg addressTxTokens: AddressTransactionTokenDbEntity)

    @Query("SELECT * FROM address_transaction_token WHERE address = :address AND tx_id = :txId")
    suspend fun loadAddressTransactionTokens(
        address: String,
        txId: String
    ): List<AddressTransactionTokenDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMultisigTransaction(vararg multisigTransactions: MultisigTransactionDbEntity)

    @Query("SELECT * FROM multisig_transaction WHERE address = :address")
    suspend fun loadMultisigTransactions(address: String): List<MultisigTransactionDbEntity>

    @Query("SELECT * FROM multisig_transaction WHERE id = :id")
    suspend fun loadMultisigTransaction(id: Long): MultisigTransactionDbEntity?

    @Query("SELECT * FROM multisig_transaction WHERE tx_id = :txId AND address = :address")
    suspend fun loadMultisigTransaction(txId: String, address: String): MultisigTransactionDbEntity?

    @Query("DELETE FROM multisig_transaction WHERE address = :address")
    suspend fun deleteMulisigTransactions(address: String)
}