package org.ergoplatform.persistance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightTokenDbProvider(private val appDatabase: AppDatabase) : TokenDbProvider {
    override suspend fun loadTokenPrices(): List<TokenPrice> {
        return withContext(Dispatchers.IO) {
            appDatabase.tokenPriceQueries.loadAll().executeAsList().map { it.toModel() }
        }
    }

    override suspend fun updateTokenPrices(priceList: List<TokenPrice>) {
        withContext(Dispatchers.IO) {
            appDatabase.transaction {
                appDatabase.tokenPriceQueries.deletAll()
                priceList.forEach {
                    appDatabase.tokenPriceQueries.insertOrReplace(it.toDbEntity())
                }
            }
        }
    }

    override suspend fun loadTokenInformation(tokenId: String): TokenInformation? {
        return withContext(Dispatchers.IO) {
            appDatabase.tokenInfoQueries.loadById(tokenId).executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation) {
        withContext(Dispatchers.IO) {
            appDatabase.tokenInfoQueries.insertOrReplace(tokenInfo.toDbEntity())
        }
    }

    override suspend fun pruneUnusedTokenInformation() {
        withContext(Dispatchers.IO) {
            appDatabase.tokenInfoQueries.pruneUnused(System.currentTimeMillis() - TOKEN_INFO_MS_OUTDATED)
        }
    }
}