package org.ergoplatform.tokens

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.api.ErgoExplorerApi
import org.ergoplatform.api.TokenCheckResponse
import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.appkit.impl.Eip4TokenBuilder
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.persistance.*
import org.ergoplatform.utils.LogUtils

class TokenInfoManager {
    /**
     * @returns Flow with token information, from db if possible, from Explorer API if not available
     *              in DB. If updates need to be done, the Flow might emit updates at a later time
     */
    fun getTokenInformationFlow(
        tokenId: String,
        tokenDbProvider: TokenDbProvider,
        apiService: ApiServiceManager
    ): Flow<TokenInformation?> {
        return flow {
            val emittedToken = withContext(Dispatchers.IO) {
                // load from DB; if not found, try to load from API
                loadTokenFromDbOrApi(tokenDbProvider, tokenId, apiService)
            }
            // emit what we've found, null if there was an error
            emit(emittedToken)

            // if necessary, update token information and emit again
            emittedToken?.let {
                withContext(Dispatchers.IO) {
                    updateTokenInformationWhenNecessary(emittedToken, apiService, tokenDbProvider)
                }?.let { emit(it) }
            }
        }
    }

    /**
     * returns token information, updated if necessary.
     */
    suspend fun getTokenInformation(
        tokenId: String,
        tokenDbProvider: TokenDbProvider,
        apiService: ApiServiceManager
    ): TokenInformation? {
        return withContext(Dispatchers.IO) {
            val fromDB = loadTokenFromDbOrApi(tokenDbProvider, tokenId, apiService)
            val updated = fromDB?.let { updateTokenInformationWhenNecessary(it, apiService, tokenDbProvider) }

            return@withContext updated ?: fromDB
        }
    }

    /**
     * loads token information from DB. If not present, tries to load from API and inserts into db
     */
    private suspend fun loadTokenFromDbOrApi(
        tokenDbProvider: TokenDbProvider,
        tokenId: String,
        apiService: ErgoExplorerApi
    ) = tokenDbProvider.loadTokenInformation(tokenId) ?: try {
        val tokenFromApi = fetchTokenInformationFromApi(apiService, tokenId)
        tokenDbProvider.insertOrReplaceTokenInformation(tokenFromApi)
        tokenFromApi
    } catch (t: Throwable) {
        LogUtils.logDebug(
            "TokenInfoManager",
            "Could not fetch information for token $tokenId",
            t
        )
        null
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateTokenInformationWhenNecessary(
        token: TokenInformation,
        apiService: ApiServiceManager,
        tokenDbProvider: TokenDbProvider
    ): TokenInformation? {
        return if (System.currentTimeMillis() - token.updatedMs > 1000L * 60 * 60) {

            // check if genuine
            val tokenVerifyResponse = try {
                val checkTokenCall = apiService.checkToken(token.tokenId, token.displayName).execute()
                if (!checkTokenCall.isSuccessful)
                    throw IllegalStateException(checkTokenCall.errorBody()!!.string())
                checkTokenCall.body()!!
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "Error verifying token: ${t.message}",
                    t
                )
                TokenCheckResponse(GENUINE_UNKNOWN, null)
            }

            // check for NFT
            val thumbnailType =
                if (token.thumbnailType == THUMBNAIL_TYPE_BYTES_PNG && token.thumbnailBytes != null)
                    THUMBNAIL_TYPE_BYTES_PNG else
                    try {
                        val eip4 = token.toEip4Token()

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
                tokenVerifyResponse.genuine,
                if (tokenVerifyResponse.genuine == GENUINE_VERIFIED) tokenVerifyResponse.token?.issuer else null,
                token.thumbnailBytes,
                thumbnailType,
                timeNow
            )

            GlobalScope.launch(Dispatchers.IO) {
                tokenDbProvider.insertOrReplaceTokenInformation(newToken)
                tokenDbProvider.pruneUnusedTokenInformation()
            }

            newToken
        } else null
    }

    private fun fetchTokenInformationFromApi(
        apiService: ErgoExplorerApi,
        tokenId: String
    ): TokenInformation {
        LogUtils.logDebug(
            "TokenInfoManager",
            "Load information for token $tokenId from API"
        )

        val tokenApiResponse = apiService.getTokenInformation(tokenId).execute()

        if (!tokenApiResponse.isSuccessful) {
            throw IllegalStateException("No token information available for $tokenId")
        }

        val issuingBoxId = tokenApiResponse.body()!!.boxId

        val boxInfo: OutputInfo = apiService.getExplorerBoxInformation(issuingBoxId).execute().body()!!

        val tokenInfo = boxInfo.assets.find { it.tokenId == tokenId }!!

        val eip4Token = Eip4TokenBuilder.buildFromAdditionalRegisters(
            tokenId,
            tokenInfo.amount,
            boxInfo.additionalRegisters
        )

        return TokenInformation(
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