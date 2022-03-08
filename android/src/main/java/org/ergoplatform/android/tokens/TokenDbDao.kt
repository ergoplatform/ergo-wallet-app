package org.ergoplatform.android.tokens

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TokenDbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenPrices(vararg tokenPrices: TokenPriceDbEntity)

    @Query("DELETE FROM token_price")
    suspend fun deleteAllTokenPrices()

    @Query("SELECT * FROM token_price")
    suspend fun getAllTokenPrices(): List<TokenPriceDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTokenInformation(vararg tokenInfo: TokenInformationDbEntity)

    @Query("DELETE FROM token_info WHERE updated_ms < :thresholdMs")
    suspend fun deleteOutdatedTokenInformation(thresholdMs: Long)

    @Query("SELECT * FROM token_info WHERE tokenId = :tokenId")
    suspend fun getTokenInformation(tokenId: String): TokenInformationDbEntity?

}