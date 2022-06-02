package org.ergoplatform.android.mosaik

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MosaikDbDao {
    @Query("SELECT * FROM mosaik_app WHERE url == :url")
    suspend fun loadAppEntry(url: String): MosaikAppDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppEntry(vararg mosaikApp: MosaikAppDbEntity)

    @Query("SELECT * FROM mosaik_app WHERE favorite != 0  ORDER BY last_visited DESC")
    fun getAppFavoritesByLastVisited(): Flow<List<MosaikAppDbEntity>>

    @Query("SELECT * FROM mosaik_app WHERE favorite == 0 ORDER BY last_visited DESC LIMIT :limit")
    fun getAllAppsByLastVisited(limit: Int): Flow<List<MosaikAppDbEntity>>

    @Query("DELETE FROM mosaik_app WHERE favorite == 0 AND last_visited < :timestamp")
    suspend fun deleteAppsNotFavoriteVisitedBefore(timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppHost(vararg mosaikApp: MosaikHostDbEntity)

    @Query("SELECT * FROM mosaik_host WHERE hostName = :hostname")
    suspend fun getMosaikHostInfo(hostname: String): MosaikHostDbEntity?

    // TODO Mosaik delete host info
}