package org.ergoplatform

import org.ergoplatform.api.*
import org.ergoplatform.explorer.client.DefaultApi
import org.ergoplatform.explorer.client.model.*
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.restapi.client.Transactions
import org.ergoplatform.restapi.client.TransactionsApi
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

open class ApiServiceManager(
    private val defaultApi: DefaultApi,
    private val nodeTransactionsApi: TransactionsApi,
    private val tokenVerificationApi: TokenVerificationApi
) : ErgoExplorerApi, TokenVerificationApi, ErgoNodeApi {

    override fun getTotalBalanceForAddress(publicAddress: String): Call<TotalBalance> =
        defaultApi.getApiV1AddressesP1BalanceTotal(publicAddress)

    override fun getBoxInformation(boxId: String): Call<OutputInfo> =
        defaultApi.getApiV1BoxesP1(boxId)

    override fun getTokenInformation(tokenId: String): Call<TokenInfo> =
        defaultApi.getApiV1TokensP1(tokenId)

    override fun getTransactionInformation(txId: String): Call<TransactionInfo> =
        defaultApi.getApiV1TransactionsP1(txId)

    // this is the Ergo Explorer call
    override fun getMempoolTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>> =
        defaultApi.getApiV1MempoolTransactionsByaddressP1(publicAddress, offset, limit)

    // this is the Node API call
    override fun getUnconfirmedTransactions(limit: Int): Call<Transactions> =
        nodeTransactionsApi.getUnconfirmedTransactions(limit, 0)

    override fun getConfirmedTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>> =
        // TODO concise should be true when https://github.com/ergoplatform/explorer-backend/issues/193 is fixed
        defaultApi.getApiV1AddressesP1Transactions(publicAddress, offset, limit, false)

    override fun checkToken(tokenId: String, tokenName: String): Call<TokenCheckResponse> =
        tokenVerificationApi.checkToken(tokenId, tokenName)

    companion object {
        private var ergoApiService: ApiServiceManager? = null

        fun getOrInit(preferences: PreferencesProvider): ApiServiceManager {
            if (ergoApiService == null) {

                val retrofitExplorer = Retrofit.Builder()
                    .baseUrl(preferences.prefExplorerApiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpSingleton.getInstance())
                    .build()
                val defaultApi = retrofitExplorer.create(DefaultApi::class.java)

                val retrofitNode = Retrofit.Builder()
                    .baseUrl(preferences.prefNodeUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpSingleton.getInstance())
                    .build()
                val nodeTransactionsApi = retrofitNode.create(TransactionsApi::class.java)

                val retrofitTokenVerify = Retrofit.Builder()
                    .baseUrl(preferences.prefTokenVerificationUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpSingleton.getInstance())
                    .build()
                val tokenVerificationApi =
                    retrofitTokenVerify.create(TokenVerificationApi::class.java)

                ergoApiService = ApiServiceManager(
                    defaultApi,
                    nodeTransactionsApi,
                    tokenVerificationApi
                )
            }
            return ergoApiService!!
        }


        fun resetApiService() {
            ergoApiService = null
        }


    }
}