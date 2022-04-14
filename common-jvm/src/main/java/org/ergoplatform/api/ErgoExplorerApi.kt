package org.ergoplatform.api

import org.ergoplatform.explorer.client.model.*
import retrofit2.Call

interface ErgoExplorerApi {
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