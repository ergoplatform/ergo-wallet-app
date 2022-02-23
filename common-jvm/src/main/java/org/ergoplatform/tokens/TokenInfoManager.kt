package org.ergoplatform.tokens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.ErgoApiService
import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.appkit.impl.Eip4TokenBuilder
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.persistance.*
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

        fromDb?.let {
            updateTokenInformationWhenNecessary(it, tokenDbProvider, null)
        }

        return fromDb
    }

    private fun updateTokenInformationWhenNecessary(
        token: TokenInformation,
        tokenDbProvider: TokenDbProvider,
        eip4Token: Eip4Token?
    ) {
        if (System.currentTimeMillis() - token.updatedMs > 1000L * 60 * 60) {
            GlobalScope.launch(Dispatchers.IO) {

                // check if genuine
                val genuineToken = TokenVerifier.checkTokenGenuine(token.tokenId, token.displayName)

                val tokenGenuine = when {
                    genuineToken == null -> GENUINE_UNKNOWN
                    genuineToken.tokenId == token.tokenId -> GENUINE_VERIFIED
                    else -> GENUINE_SUSPICIOUS
                }

                // check for NFT
                val thumbnailType =
                    if (token.thumbnailType == THUMBNAIL_TYPE_BYTES_PNG && token.thumbnailBytes != null)
                        THUMBNAIL_TYPE_BYTES_PNG else
                        try {
                            val eip4 = eip4Token ?: token.toEip4Token()

                            when (eip4.assetType) {
                                Eip4Token.AssetType.NFT_PICTURE -> THUMBNAIL_TYPE_NFT_IMG
                                Eip4Token.AssetType.NFT_AUDIO -> THUMBNAIL_TYPE_NFT_AUDIO
                                Eip4Token.AssetType.NFT_VIDEO -> THUMBNAIL_TYPE_NFT_VID
                                else -> THUMBNAIL_TYPE_NONE
                            }
                        } catch (t: Throwable) {
                            LogUtils.logDebug("TokenInfoManager", "Error processing EIP4 token", t)
                            THUMBNAIL_TYPE_NONE
                        }

                val timeNow = System.currentTimeMillis()
                val newToken = TokenInformation(
                    token,
                    tokenGenuine,
                    if (tokenGenuine == GENUINE_VERIFIED) genuineToken!!.issuer else null,
                    token.thumbnailBytes,
                    thumbnailType,
                    timeNow
                )

                tokenDbProvider.insertOrReplaceTokenInformation(newToken)
                tokenDbProvider.pruneUnusedTokenInformation()
                lastRepoRefresh.value = timeNow
            }
        }
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
            lastRepoRefresh.value = System.currentTimeMillis()

            updateTokenInformationWhenNecessary(tokenInformation, tokenDbProvider, eip4Token)
        }

        return tokenInformation
    }

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