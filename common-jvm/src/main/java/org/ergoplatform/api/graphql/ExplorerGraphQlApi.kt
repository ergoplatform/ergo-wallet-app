package org.ergoplatform.api.graphql

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.utils.MEDIA_TYPE_JSON
import org.ergoplatform.utils.httpPostStringSync

class ExplorerGraphQlApi(
    private val url: String,
) {

    fun getUnconfirmedBoxById(boxId: String): OutputInfo? {
        val request = """{"query":"query Query {\n mempool {\n    boxes (boxId: \"$boxId\") {\n    boxId\n    address\n    value\n    ergoTree\n    creationHeight\n    index\n    transactionId\n    assets {\n      tokenId\n      amount\n    }\n    additionalRegisters\n    }\n  }\n}","variables":{},"operationName":"Query"}"""
        val responseString = httpPostStringSync(url, request, MEDIA_TYPE_JSON)

        val type = object : TypeToken<GraphQlResponse<GraphQlMempoolResponse>>() {}.type
        val response = Gson().fromJson<GraphQlResponse<GraphQlMempoolResponse>>(responseString, type)

        return response.data.mempool.boxes?.firstOrNull()?.toOutputInfo()
    }

    private fun GraphQlBox.toOutputInfo(): OutputInfo {
        val fromgql = this
        return OutputInfo().apply {
            boxId = fromgql.boxId
            address = fromgql.address
            value = fromgql.value
            ergoTree = fromgql.ergoTree
            creationHeight = fromgql.creationHeight.toInt()
            index = fromgql.index
            transactionId = fromgql.transactionId
            assets = fromgql.assets.map {  graphqlAsset ->
                AssetInstanceInfo().apply {
                    tokenId = graphqlAsset.tokenId
                    amount = graphqlAsset.amount
                }
            }
        }
    }
}

data class GraphQlResponse<R>(val data: R)

data class GraphQlMempoolResponse(val mempool: GraphQlMempool)

data class GraphQlMempool(val boxes: List<GraphQlBox>?)

data class GraphQlBox(
    val boxId: String,
    val address: String,
    val value: Long,
    val ergoTree: String,
    val creationHeight: Long,
    val index: Int,
    val transactionId: String,
    val assets: List<GraphQlAsset>,
)

data class GraphQlAsset(
    val tokenId: String,
    val amount: Long,
)