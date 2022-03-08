package org.ergoplatform.transactions

import org.ergoplatform.ErgoBox
import org.ergoplatform.UnsignedErgoLikeTransaction
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.ErgoId
import org.ergoplatform.appkit.Iso
import org.ergoplatform.appkit.impl.Eip4TokenBuilder
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.InputInfo
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.getErgoNetworkType
import scala.Tuple2
import scala.collection.JavaConversions
import special.collection.Coll
import kotlin.math.min

/**
 * describes a transaction with its ID and lists of inputs (boxes to spend) and outputs (boxes
 * to issue)
 */
data class TransactionInfo(
    val id: String,
    val inputs: List<InputInfo>,
    val outputs: List<OutputInfo>
)

/**
 * describes a box to spend or a box to issue with its box ID, address that secures the box,
 * value of nanoergs and list of tokens the box holds or will hold.
 */
data class TransactionInfoBox(
    val boxId: String,
    val address: String,
    val value: Long,
    val tokens: List<AssetInstanceInfo>
)

/**
 * @return list of IDs of boxes to spend for this `UnsignedErgoLikeTransaction`
 */
fun UnsignedErgoLikeTransaction.getInputBoxesIds(): List<String> {
    return JavaConversions.seqAsJavaList(inputs())!!.map {
        ErgoId(it.boxId()).toString()
    }
}

/**
 * @return TransactionInfo data class for this UnsignedErgoLikeTransaction. The TransactionInfo
 *         is more Kotlin-friendly to work with and holds information for the boxes to spend
 *         and more information for tokens on the boxes to issue.
 */
fun UnsignedErgoLikeTransaction.buildTransactionInfo(inputBoxes: HashMap<String, TransactionInfoBox>): TransactionInfo {
    val inputsList = ArrayList<InputInfo>()
    val outputsList = ArrayList<OutputInfo>()
    val tokensMap = HashMap<String, AssetInstanceInfo>()

    // now add to TransactionInfo, if possible
    getInputBoxesIds().forEach { boxid ->
        val inputInfo = InputInfo()
        inputInfo.boxId = boxid
        inputBoxes.get(boxid)?.let { inboxInfo ->
            inputInfo.address = inboxInfo.address
            inputInfo.value = inboxInfo.value
            inputInfo.assets = inboxInfo.tokens

            // store token information in map to use on outbox info
            inboxInfo.tokens.forEach { tokenInfo ->
                if (tokenInfo.name != null || tokenInfo.decimals != null) {
                    tokensMap.put(tokenInfo.tokenId, tokenInfo)
                }
            }
        } ?: throw java.lang.IllegalArgumentException("No information for input box $boxid")
        inputsList.add(inputInfo)
    }

    JavaConversions.seqAsJavaList(outputCandidates())!!.forEach { ergoBoxCandidate ->
        val outputInfo = OutputInfo()

        outputInfo.address =
            Address.fromErgoTree(ergoBoxCandidate.ergoTree(), getErgoNetworkType()).toString()
        outputInfo.value = ergoBoxCandidate.value()

        outputsList.add(outputInfo)

        getAssetInstanceInfosFromErgoBoxToken(ergoBoxCandidate.additionalTokens()).forEach {
            outputInfo.addAssetsItem(it)

            // if we know about the token from the inboxes, add more information
            tokensMap.get(it.tokenId)?.let { tokenInfo ->
                it.name = tokenInfo.name
                it.decimals = tokenInfo.decimals
            } ?: if (it.tokenId.equals(inputsList[0].boxId)) {
                // could be minted right here, check outbox info
                val token = try {
                    Eip4TokenBuilder.buildFromErgoBoxCandidate(it.tokenId, ergoBoxCandidate)
                } catch (t: Throwable) {
                    null
                }
                token?.let { tokenInfo ->
                    it.name = tokenInfo.tokenName
                    it.decimals = tokenInfo.decimals
                }
            }
        }
    }

    return TransactionInfo(id(), inputsList, outputsList)
}

/**
 * @return TransactionInfoBox, which is more Kotlin-friendly to work with
 */
fun ErgoBox.toTransactionInfoBox(): TransactionInfoBox {
    return TransactionInfoBox(
        ErgoId(id()).toString(),
        Address.fromErgoTree(ergoTree(), getErgoNetworkType()).toString(),
        value(),
        getAssetInstanceInfosFromErgoBoxToken(additionalTokens())
    )
}

/**
 * @return TransactionInfoBox, which is more Kotlin-friendly to work with
 */
fun OutputInfo.toTransactionInfoBox(): TransactionInfoBox {
    return TransactionInfoBox(boxId, address, value, assets)
}

private fun getAssetInstanceInfosFromErgoBoxToken(tokensColl: Coll<Tuple2<ByteArray, Any>>): List<AssetInstanceInfo> {
    val tokens = Iso.isoTokensListToPairsColl().from(tokensColl)
    return tokens.map {
        val tokenInfo = AssetInstanceInfo()
        tokenInfo.amount = it.value
        tokenInfo.tokenId = it.id.toString()

        tokenInfo
    }
}

/**
 * combines inboxes and outboxes and reduces change amounts for user-friendly outputs
 * returns a new object with less information
 */
fun TransactionInfo.reduceBoxes(): TransactionInfo {
    // combine boxes to or from same addresses
    val combinedInputs = HashMap<String, InputInfo>()

    inputs.forEach {
        val addressKey = it.address ?: it.boxId
        val combined = combinedInputs.get(addressKey) ?: InputInfo()
        combined.address = it.address
        combined.boxId = it.boxId
        combined.value = (combined.value ?: 0) + (it.value ?: 0)
        it.assets?.forEach { combined.addAssetsItem(it) }
        combinedInputs.put(addressKey, combined)
    }

    val combinedOutputs = HashMap<String, OutputInfo>()

    outputs.forEach {
        val addressKey = it.address ?: it.boxId
        val combined = combinedOutputs.get(addressKey) ?: OutputInfo()
        combined.address = it.address
        combined.boxId = it.boxId
        combined.value = (combined.value ?: 0) + (it.value ?: 0)
        it.assets?.forEach { combined.addAssetsItem(it) }
        combinedOutputs.put(addressKey, combined)
    }

    combinedInputs.values.forEach {
        if (it.assets != null) it.assets = combineTokens(it.assets)
    }
    combinedOutputs.values.forEach {
        if (it.assets != null) it.assets = combineTokens(it.assets)
    }

    // reduce change amount
    combinedInputs.values.forEach { input ->
        combinedOutputs.get(input.address ?: input.boxId)?.let { output ->
            val ergAmount = min(output.value, input.value)
            output.value = output.value - ergAmount
            input.value = input.value - ergAmount

            // reduce token amounts
            // we can operate on the list entries safely because combineTokens() copied
            // all entries of the original TransactionInfo
            input.assets?.forEach { inputAssetInfo ->
                output.assets?.filter { inputAssetInfo.tokenId.equals(it.tokenId) }?.firstOrNull()
                    ?.let { outputAssetInfo ->
                        val tokenAmount = min(inputAssetInfo.amount, outputAssetInfo.amount)
                        inputAssetInfo.amount = inputAssetInfo.amount - tokenAmount
                        outputAssetInfo.amount = outputAssetInfo.amount - tokenAmount
                    }
            }

            input.assets = input.assets?.filter { it.amount != 0L }
            output.assets = output.assets?.filter { it.amount != 0L }
        }
    }

    val inputsList = ArrayList<InputInfo>()
    combinedInputs.values.forEach {
        // it.address == null needed since we need to keep boxes with null value
        // when no input box information available
        if (it.value > 0 || !it.assets.isNullOrEmpty() || it.address == null)
            inputsList.add(it)
    }

    val outputsList = ArrayList<OutputInfo>()
    combinedOutputs.values.forEach {
        if (it.value > 0 || !it.assets.isNullOrEmpty())
            outputsList.add(it)
    }

    return TransactionInfo(id, inputsList, outputsList)
}

/**
 * combine tokens with same id but different list entries
 * returned items are guaranteed to be a copy
 */
fun combineTokens(tokens: List<AssetInstanceInfo>): List<AssetInstanceInfo> {
    val hashmap = HashMap<String, AssetInstanceInfo>()

    tokens.forEach {
        val combined = hashmap.get(it.tokenId) ?: AssetInstanceInfo()
        combined.tokenId = it.tokenId
        combined.decimals = it.decimals
        combined.name = it.name
        combined.amount = (combined.amount ?: 0) + it.amount
        combined.type = it.type
        hashmap.put(it.tokenId, combined)
    }

    return hashmap.values.toList()
}
