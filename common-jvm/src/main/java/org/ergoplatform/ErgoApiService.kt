package org.ergoplatform

import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.explorer.client.DefaultApi
import org.ergoplatform.explorer.client.model.*
import org.ergoplatform.persistance.PreferencesProvider
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface ErgoApi {
    fun getTotalBalanceForAddress(publicAddress: String): Call<TotalBalance>
    fun getBoxInformation(boxId: String): Call<OutputInfo>
    fun getTokenInformation(tokenId: String): Call<TokenInfo>
    fun getTransactionInformation(txId: String): Call<TransactionInfo>
    fun getMempoolTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>>

    fun getConfirmedTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>>
}

class ErgoApiService(val defaultApi: DefaultApi) : ErgoApi {

    override fun getTotalBalanceForAddress(publicAddress: String): Call<TotalBalance> =
        defaultApi.getApiV1AddressesP1BalanceTotal(publicAddress)

    override fun getBoxInformation(boxId: String): Call<OutputInfo> =
        defaultApi.getApiV1BoxesP1(boxId)

    override fun getTokenInformation(tokenId: String): Call<TokenInfo> =
        defaultApi.getApiV1TokensP1(tokenId)

    override fun getTransactionInformation(txId: String): Call<TransactionInfo> =
        defaultApi.getApiV1TransactionsP1(txId)

    override fun getMempoolTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>> =
        defaultApi.getApiV1MempoolTransactionsByaddressP1(publicAddress, offset, limit)

    override fun getConfirmedTransactionsForAddress(
        publicAddress: String,
        limit: Int,
        offset: Int
    ): Call<Items<TransactionInfo>> =
        // TODO concise should be true when https://github.com/ergoplatform/explorer-backend/issues/193 is fixed
        defaultApi.getApiV1AddressesP1Transactions(publicAddress, offset, limit, false)

    companion object {
        private var ergoApiService: ErgoApiService? = null

        fun getOrInit(preferences: PreferencesProvider): ErgoApiService {
            if (ergoApiService == null) {

                val retrofit = Retrofit.Builder()
                    .baseUrl(preferences.prefExplorerApiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpSingleton.getInstance())
                    .build()

                val defaultApi = retrofit.create(DefaultApi::class.java)
                ergoApiService = ErgoApiService(defaultApi)
            }
            return ergoApiService!!
        }


        fun resetApiService() {
            ergoApiService = null
        }


    }
}