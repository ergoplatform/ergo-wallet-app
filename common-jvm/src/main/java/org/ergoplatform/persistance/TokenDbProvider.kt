package org.ergoplatform.persistance

import kotlinx.coroutines.flow.Flow

interface TokenDbProvider {
    // Price functions
    suspend fun loadTokenPrices(): List<TokenPrice>
    suspend fun updateTokenPrices(priceList: List<TokenPrice>)

    // token information functions
    suspend fun loadTokenInformation(tokenId: String): TokenInformation
    suspend fun getTokenInformationFlow(tokenId: String): Flow<TokenInformation>
    suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation)
    suspend fun pruneUnusedTokenInformation()
}