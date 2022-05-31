package org.ergoplatform.mosaik

interface MosaikDbProvider {
    suspend fun insertOrUpdateAppEntry(mosaikApp: MosaikAppEntry)

    suspend fun getAllAppFavorites(): List<MosaikAppEntry>

    suspend fun getAllAppsByLastVisited(): List<MosaikAppEntry>

    suspend fun insertOrUpdateAppHost(mosaikApp: MosaikAppHost)

    suspend fun getMosaikHostInfo(hostname: String): MosaikAppHost?
}