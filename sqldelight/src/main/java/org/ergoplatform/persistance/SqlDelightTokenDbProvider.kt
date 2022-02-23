package org.ergoplatform.persistance

class SqlDelightTokenDbProvider(private val appDatabase: AppDatabase): TokenDbProvider {
    override suspend fun loadTokenPrices(): List<TokenPrice> {
        TODO("Not yet implemented")
    }

    override suspend fun updateTokenPrices(priceList: List<TokenPrice>) {
        TODO("Not yet implemented")
    }

    override suspend fun loadTokenInformation(tokenId: String): TokenInformation? {
        TODO("Not yet implemented")
    }

    override suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation) {
        TODO("Not yet implemented")
    }

    override suspend fun pruneUnusedTokenInformation() {
        TODO("Not yet implemented")
    }
}