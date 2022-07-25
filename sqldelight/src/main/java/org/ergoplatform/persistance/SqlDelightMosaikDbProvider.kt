package org.ergoplatform.persistance

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ergoplatform.mosaik.MosaikAppEntry
import org.ergoplatform.mosaik.MosaikAppHost
import org.ergoplatform.mosaik.MosaikDbProvider

class SqlDelightMosaikDbProvider(private val sqlDelightAppDb: SqlDelightAppDb) : MosaikDbProvider {
    private val appDatabase = sqlDelightAppDb.appDatabase

    override suspend fun loadAppEntry(url: String): MosaikAppEntry? =
        sqlDelightAppDb.useIoContext {
            appDatabase.mosaikAppQueries.loadAppEntry(url).executeAsOneOrNull()?.toModel()
        }

    override suspend fun insertOrUpdateAppEntry(mosaikApp: MosaikAppEntry) {
        sqlDelightAppDb.useIoContext {
            appDatabase.mosaikAppQueries.insertOrReplace(mosaikApp.toDbEntity())
        }
    }

    override fun getAllAppFavoritesByLastVisited(): Flow<List<MosaikAppEntry>> =
        appDatabase.mosaikAppQueries.appFavoritesByLastVisited().asFlow().mapToList()
            .map { flow -> flow.map { it.toModel() } }

    override fun getAllAppsByLastVisited(limit: Int): Flow<List<MosaikAppEntry>> =
        appDatabase.mosaikAppQueries.allAppsByLastVisited().asFlow().mapToList()
            .map { flow -> flow.take(limit).map { it.toModel() } }

    override suspend fun deleteAppsNotFavoriteVisitedBefore(timestamp: Long) {
        sqlDelightAppDb.useIoContext {
            appDatabase.mosaikAppQueries.deleteOldest(timestamp)
        }
    }

    override suspend fun insertOrUpdateAppHost(mosaikApp: MosaikAppHost) {
        sqlDelightAppDb.useIoContext {
            appDatabase.mosaikHostQueries.insertOrReplace(mosaikApp.toDbEntity())
        }
    }

    override suspend fun getMosaikHostInfo(hostname: String): MosaikAppHost? =
        sqlDelightAppDb.useIoContext {
            appDatabase.mosaikHostQueries.getHostInfo(hostname).executeAsOneOrNull()?.toModel()
        }

}
