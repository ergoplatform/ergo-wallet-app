package org.ergoplatform.persistance

const val TOKEN_INFO_MS_OUTDATED = 1000L * 60 * 60 * 24 * 30

interface TokenDbProvider {

    /**
     *  P R I C E    F U N C T I O N S
     */

    /**
     * loads all available persisted [TokenPrice] entities
     */
    suspend fun loadTokenPrices(): List<TokenPrice>

    /**
     * updates all TokenPrice entities, replacing and deleting all existing ones
     */
    suspend fun updateTokenPrices(priceList: List<TokenPrice>)



    /**
     *  T O K E N    I N F O R M A T I O N    F U N C T I O N S
     */

    /**
     * loads [TokenInformation] entity of a token of given tokenId, if available
     */
    suspend fun loadTokenInformation(tokenId: String): TokenInformation?

    /**
     * saves [TokenInformation], replacing any already existing information with the same token id
     */
    suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation)

    /**
     * deletes all outdated [TokenInformation] from database to free up space. Outdated information
     * is information not updated since [TOKEN_INFO_MS_OUTDATED]
     */
    suspend fun pruneUnusedTokenInformation()
}