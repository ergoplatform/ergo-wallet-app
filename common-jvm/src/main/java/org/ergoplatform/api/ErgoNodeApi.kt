package org.ergoplatform.api

import org.ergoplatform.restapi.client.Transactions
import retrofit2.Call

interface ErgoNodeApi {
    fun getUnconfirmedTransactions(limit: Int): Call<Transactions>
}