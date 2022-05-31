package org.ergoplatform.android.mosaik

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MosaikDbDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppEntry(vararg mosaikApp: MosaikAppDbEntity)

    @Query("SELECT * FROM mosaik_app WHERE favorite != 0")
    suspend fun getAllAppFavorites(): List<MosaikAppDbEntity>

    @Query("SELECT * FROM mosaik_app WHERE favorite == 0 ORDER BY last_visited")
    suspend fun getAllAppsByLastVisited(): List<MosaikAppDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppHost(vararg mosaikApp: MosaikHostDbEntity)

    @Query("SELECT * FROM mosaik_host WHERE hostName = :hostname")
    suspend fun getMosaikHostInfo(hostname: String): MosaikHostDbEntity?
}