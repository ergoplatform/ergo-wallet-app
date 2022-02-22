package org.ergoplatform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.appkit.impl.Eip4TokenBuilder
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.utils.LogUtils

object TokenInfoManager {
    /**
     * @returns token information, from db if possible, from Explorer API if not available in DB
     *          updatable information might not be loaded or outdated
     */
    suspend fun getTokenInformation(
        tokenId: String,
        apiService: ErgoApiService
    ): TokenInformationAndEip4Token? {
        // TODO load from DB first and construct

        // load from API
        return withContext(Dispatchers.IO) {
            try {
                fetchTokenInformationFromApi(apiService, tokenId)

                // TODO insert into db
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    "TokenInfoManager",
                    "Could not fetch information for token $tokenId",
                    t
                )
                null
            }
        }
    }

    private fun fetchTokenInformationFromApi(
        apiService: ErgoApiService,
        tokenId: String
    ): TokenInformationAndEip4Token {
        val defaultApi = apiService.defaultApi
        val tokenApiResponse = defaultApi.getApiV1TokensP1(tokenId).execute()

        if (!tokenApiResponse.isSuccessful) {
            throw IllegalStateException("No token information available for $tokenId")
        }

        val issuingBoxId = tokenApiResponse.body()!!.boxId

        val boxInfo: OutputInfo =
            defaultApi.getApiV1BoxesP1(issuingBoxId).execute().body()!!

        val tokenInfo = boxInfo.assets.find { it.tokenId == tokenId }!!

        val eip4Token = Eip4TokenBuilder.buildFromRegisters(
            tokenId,
            tokenInfo.amount,
            boxInfo.additionalRegisters
        )

        return TokenInformationAndEip4Token(
            eip4Token, TokenInformation(
                tokenId,
                issuingBoxId,
                boxInfo.transactionId,
                eip4Token.tokenName,
                eip4Token.tokenDescription,
                eip4Token.decimals,
                eip4Token.value,
                eip4Token.mintingBoxR7?.toHex(),
                eip4Token.mintingBoxR8?.toHex(),
                eip4Token.mintingBoxR9?.toHex()
            )
        )
    }

    /**
     * EIP4Token can be constructed from TokenInformation, but as this is a costly operation we
     * pass both objects around together
     */
    data class TokenInformationAndEip4Token(
        val eip4Token: Eip4Token,
        val tokenInformation: TokenInformation
    )
}