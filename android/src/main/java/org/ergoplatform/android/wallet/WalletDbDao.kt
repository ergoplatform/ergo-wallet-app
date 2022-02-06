package org.ergoplatform.android.wallet

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg walletConfigs: WalletConfigDbEntity)

    @Query("DELETE FROM wallet_configs WHERE id = :walletId")
    suspend fun deleteWalletConfig(walletId: Int)

    @Query("DELETE FROM wallet_states WHERE wallet_first_address = :firstAddress")
    suspend fun deleteWalletStates(firstAddress: String)

    @Query("DELETE FROM wallet_states WHERE public_address = :publicAddress")
    suspend fun deleteAddressState(publicAddress: String)

    @Query("DELETE FROM wallet_tokens WHERE public_address = :publicAddress")
    suspend fun deleteTokensByAddress(publicAddress: String)

    @Query("DELETE FROM wallet_tokens WHERE wallet_first_address = :firstAddress")
    suspend fun deleteTokensByWallet(firstAddress: String)

    @Query("DELETE FROM wallet_addresses WHERE wallet_first_address = :firstAddress")
    suspend fun deleteWalletAddresses(firstAddress: String)

    @Update
    suspend fun update(walletConfig: WalletConfigDbEntity)

    // used to only update unfoldTokens field while keeping the sensitive information save
    // when more fields to update are needed in the future change to target entities:
    //    https://stackoverflow.com/a/59834309/7487013
    // not done for now because overhead for a single field
    @Query("UPDATE wallet_configs SET unfold_tokens = :unfoldTokens WHERE id = :id")
    suspend fun updateWalletTokensUnfold(id: Int, unfoldTokens: Boolean)

    @Query("SELECT * FROM wallet_configs WHERE id = :id")
    suspend fun loadWalletConfigById(id: Int): WalletConfigDbEntity?

    @Transaction
    @Query("SELECT * FROM wallet_configs WHERE id = :id")
    suspend fun loadWalletWithStateById(id: Int): WalletDbEntity?

    @Query("SELECT * FROM wallet_configs WHERE id = :id")
    fun walletWithStateByIdAsFlow(id: Int): Flow<WalletDbEntity?>

    @Query("SELECT * FROM wallet_configs WHERE public_address = :firstAddress")
    suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfigDbEntity?

    @Query("SELECT * FROM wallet_addresses WHERE wallet_first_address = :firstAddress")
    suspend fun loadWalletAddresses(firstAddress: String): List<WalletAddressDbEntity>

    @Query("SELECT * FROM wallet_addresses WHERE id = :id")
    suspend fun loadWalletAddress(id: Int): WalletAddressDbEntity?

    @Query("SELECT * FROM wallet_addresses WHERE public_address = :publicAddress")
    suspend fun loadWalletAddress(publicAddress: String): WalletAddressDbEntity?

    @Query("UPDATE wallet_addresses SET label = :label WHERE id = :id")
    suspend fun updateWalletAddressLabel(id: Int, label: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletAddress(walletAddress: WalletAddressDbEntity)

    @Query("DELETE FROM wallet_addresses WHERE id = :id")
    suspend fun deleteWalletAddress(id: Int)

    @Query("SELECT * FROM wallet_configs")
    fun getAllWalletConfigsSyncronous(): List<WalletConfigDbEntity>

    @Transaction
    @Query("SELECT * FROM wallet_configs")
    fun getWalletsWithStates(): LiveData<List<WalletDbEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletStates(vararg walletStates: WalletStateDbEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTokens(vararg walletTokens: WalletTokenDbEntity)

}