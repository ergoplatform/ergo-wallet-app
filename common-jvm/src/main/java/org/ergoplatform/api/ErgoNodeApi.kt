package org.ergoplatform.api

import org.ergoplatform.restapi.client.BlockchainToken
import org.ergoplatform.restapi.client.BlockchainTransaction
import org.ergoplatform.restapi.client.ErgoTransaction
import org.ergoplatform.restapi.client.ErgoTransactionOutput
import org.ergoplatform.restapi.client.Transactions
import retrofit2.Call

interface ErgoNodeApi {
    fun getUnconfirmedTransactions(limit: Int): Call<Transactions>

    fun getExpectedWaitTime(fee: Long, txSize: Int): Call<Long>

    fun getSuggestedFee(waitTime: Int, txSize: Int): Call<Int>

    fun getNodeUnspentBoxInformation(boxId: String): Call<ErgoTransactionOutput>

    fun getTokenInfoNode(tokenId: String): Call<BlockchainToken>

    fun getNodeBoxInformation(boxId: String): Call<ErgoTransactionOutput>

    fun getTransactionInformationNode(txId: String): Call<BlockchainTransaction>
    fun getTransactionInformationUncomfirmedNode(txId: String): Call<ErgoTransaction>
}