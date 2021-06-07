package org.ergoplatform.android.wallet

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg walletConfigs: WalletConfigDbEntity)

    @Delete
    suspend fun delete(walletConfig: WalletConfigDbEntity)

    @Update
    suspend fun update(walletConfig: WalletConfigDbEntity)

    @Query("SELECT * FROM wallet_configs WHERE id = :id")
    suspend fun loadWalletById(id: Int): WalletConfigDbEntity

    @Query("SELECT * FROM wallet_configs")
    fun getAllSync(): List<WalletConfigDbEntity>

    @Query("SELECT * FROM wallet_configs")
    fun getAllLiveData(): LiveData<List<WalletConfigDbEntity>>

    @Transaction
    @Query("SELECT * FROM wallet_configs")
    fun getWalletsWithStates(): LiveData<List<WalletDbEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletStates(vararg walletStates: WalletStateDbEntity)

}