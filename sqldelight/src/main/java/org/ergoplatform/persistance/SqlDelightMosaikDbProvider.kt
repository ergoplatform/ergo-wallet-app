package org.ergoplatform.persistance

import kotlinx.coroutines.flow.Flow
import org.ergoplatform.mosaik.MosaikAppEntry
import org.ergoplatform.mosaik.MosaikAppHost
import org.ergoplatform.mosaik.MosaikDbProvider

class SqlDelightMosaikDbProvider(sqlDelightAppDb: SqlDelightAppDb) : MosaikDbProvider {
    override suspend fun loadAppEntry(url: String): MosaikAppEntry? {
        TODO("Not yet implemented")
    }

    override suspend fun insertOrUpdateAppEntry(mosaikApp: MosaikAppEntry) {
        TODO("Not yet implemented")
    }

    override fun getAllAppFavoritesByLastVisited(): Flow<List<MosaikAppEntry>> {
        TODO("Not yet implemented")
    }

    override fun getAllAppsByLastVisited(limit: Int): Flow<List<MosaikAppEntry>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAppsNotFavoriteVisitedBefore(timestamp: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun insertOrUpdateAppHost(mosaikApp: MosaikAppHost) {
        TODO("Not yet implemented")
    }

    override suspend fun getMosaikHostInfo(hostname: String): MosaikAppHost? {
        TODO("Not yet implemented")
    }

}
