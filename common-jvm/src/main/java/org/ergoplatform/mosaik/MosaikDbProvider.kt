package org.ergoplatform.mosaik

import kotlinx.coroutines.flow.Flow

interface MosaikDbProvider {
    suspend fun loadAppEntry(url: String): MosaikAppEntry?

    suspend fun insertOrUpdateAppEntry(mosaikApp: MosaikAppEntry)

    fun getAllAppFavoritesByLastVisited(): Flow<List<MosaikAppEntry>>

    fun getAllAppsByLastVisited(limit: Int): Flow<List<MosaikAppEntry>>

    suspend fun deleteAppsNotFavoriteVisitedBefore(timestamp: Long)

    suspend fun insertOrUpdateAppHost(mosaikApp: MosaikAppHost)

    suspend fun getMosaikHostInfo(hostname: String): MosaikAppHost?
}