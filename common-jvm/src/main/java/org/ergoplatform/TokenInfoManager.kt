package org.ergoplatform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.appkit.impl.Eip4TokenBuilder
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.persistance.TokenDbProvider
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.utils.LogUtils

class TokenInfoManager {
    val lastRepoRefresh: MutableStateFlow<Long> = MutableStateFlow(0)

    /**
     * @returns token information, from db if possible, from Explorer API if not available in DB
     *          updatable information might not be loaded or outdated
     */
    suspend fun getTokenInformation(
        tokenId: String,
        tokenDbProvider: TokenDbProvider,
        apiService: ErgoApiService
    ): TokenInformation? {
        return withContext(Dispatchers.IO) {
            getTokenInformationFromDb(tokenId, tokenDbProvider) ?:
            // load from API
            try {
                fetchTokenInformationFromApi(apiService, tokenDbProvider, tokenId)
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

    private suspend fun getTokenInformationFromDb(
        tokenId: String,
        tokenDbProvider: TokenDbProvider
    ): TokenInformation? {
        val fromDb = tokenDbProvider.loadTokenInformation(tokenId)

        // TODO mark for update if necessary
        return fromDb
    }

    private fun fetchTokenInformationFromApi(
        apiService: ErgoApiService,
        tokenDbProvider: TokenDbProvider,
        tokenId: String
    ): TokenInformation {
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

        // TODO fill more fields with Eip4Token Information

        val tokenInformation = TokenInformation(
            tokenId,
            issuingBoxId,
            boxInfo.transactionId,
            eip4Token.tokenName,
            eip4Token.tokenDescription,
            eip4Token.decimals,
            eip4Token.value,
            eip4Token.mintingBoxR7?.toHex(),
            eip4Token.mintingBoxR8?.toHex(),
            eip4Token.mintingBoxR9?.toHex(),
        )

        GlobalScope.launch(Dispatchers.IO) {
            tokenDbProvider.insertOrReplaceTokenInformation(tokenInformation)

            // TODO mark for update
        }

        return tokenInformation
    }

    // TODO prune old objects after an update

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: TokenInfoManager? = null

        fun getInstance(): TokenInfoManager {
            return instance ?: synchronized(this) {
                instance ?: TokenInfoManager().also { instance = it }
            }
        }

    }
}