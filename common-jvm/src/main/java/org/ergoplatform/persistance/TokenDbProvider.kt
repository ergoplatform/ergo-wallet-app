package org.ergoplatform.persistance

const val TOKEN_INFO_MS_OUTDATED = 1000L * 60 * 60 * 24 * 30

interface TokenDbProvider {

        // Price functions
    suspend fun loadTokenPrices(): List<TokenPrice>
    suspend fun updateTokenPrices(priceList: List<TokenPrice>)

    // token information functions
    suspend fun loadTokenInformation(tokenId: String): TokenInformation?
    suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation)
    suspend fun pruneUnusedTokenInformation()
}