package org.ergoplatform.mosaik

interface MosaikDbProvider {
    suspend fun loadAppEntry(url: String): MosaikAppEntry?

    suspend fun insertOrUpdateAppEntry(mosaikApp: MosaikAppEntry)

    suspend fun getAllAppFavorites(): List<MosaikAppEntry>

    suspend fun getAllAppsByLastVisited(limit: Int): List<MosaikAppEntry>

    suspend fun deleteAppsNotFavoriteVisitedBefore(timestamp: Long)

    suspend fun insertOrUpdateAppHost(mosaikApp: MosaikAppHost)

    suspend fun getMosaikHostInfo(hostname: String): MosaikAppHost?
}