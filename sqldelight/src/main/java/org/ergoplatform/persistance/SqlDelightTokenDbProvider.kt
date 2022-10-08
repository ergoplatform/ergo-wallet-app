package org.ergoplatform.persistance

class SqlDelightTokenDbProvider(private val sqlDelightAppDb: SqlDelightAppDb) : TokenDbProvider {
    private val appDatabase = sqlDelightAppDb.appDatabase

    override suspend fun loadTokenPrices(): List<TokenPrice> {
        return sqlDelightAppDb.useIoContext {
            appDatabase.tokenPriceQueries.loadAll().executeAsList().map { it.toModel() }
        }
    }

    override suspend fun updateTokenPrices(priceList: List<TokenPrice>) {
        sqlDelightAppDb.useIoContext {
            appDatabase.transaction {
                appDatabase.tokenPriceQueries.deletAll()
                priceList.forEach {
                    appDatabase.tokenPriceQueries.insertOrReplace(it.toDbEntity())
                }
            }
        }
    }

    override suspend fun loadTokenInformation(tokenId: String): TokenInformation? {
        return sqlDelightAppDb.useIoContext {
            appDatabase.tokenInfoQueries.loadById(tokenId).executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation) {
        sqlDelightAppDb.useIoContext {
            appDatabase.tokenInfoQueries.insertOrReplace(tokenInfo.toDbEntity())
        }
    }

    override suspend fun pruneUnusedTokenInformation() {
        sqlDelightAppDb.useIoContext {
            appDatabase.tokenInfoQueries.pruneUnused(System.currentTimeMillis() - TOKEN_INFO_MS_OUTDATED)
        }
    }
}