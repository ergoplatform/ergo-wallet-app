package org.ergoplatform.api

import org.ergoplatform.restapi.client.ErgoTransactionOutput
import org.ergoplatform.restapi.client.Transactions
import retrofit2.Call

interface ErgoNodeApi {
    fun getUnconfirmedTransactions(limit: Int): Call<Transactions>

    fun getExpectedWaitTime(fee: Long, txSize: Int): Call<Long>

    fun getSuggestedFee(waitTime: Int, txSize: Int): Call<Int>

    fun getNodeUnspentBoxInformation(boxId: String): Call<ErgoTransactionOutput>
}