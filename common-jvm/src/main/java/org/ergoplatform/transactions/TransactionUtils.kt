package org.ergoplatform.transactions

import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.InputInfo
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.explorer.client.model.TransactionInfo
import kotlin.math.min

/**
 * combines inboxes and outboxes and reduces change amounts for user-friendly outputs
 * returns a new object with less information
 * TODO #13 use own db entity data class (null safe)
 */
fun TransactionInfo.reduceBoxes(): TransactionInfo {
    val retVal = TransactionInfo()

    retVal.id = id

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
                output.assets?.filter { inputAssetInfo.tokenId.equals(it.tokenId) }?.firstOrNull()?.let {
                    outputAssetInfo ->
                    val tokenAmount = min(inputAssetInfo.amount, outputAssetInfo.amount)
                    inputAssetInfo.amount = inputAssetInfo.amount - tokenAmount
                    outputAssetInfo.amount = outputAssetInfo.amount - tokenAmount
                }
            }

            input.assets = input.assets?.filter { it.amount != 0L }
            output.assets = output.assets?.filter { it.amount != 0L }
        }
    }

    combinedInputs.values.forEach {
        // it.address == null needed since we need to keep boxes with null value
        // when no input box information available
        if (it.value > 0 || !it.assets.isNullOrEmpty() || it.address == null)
            retVal.addInputsItem(it)
    }
    combinedOutputs.values.forEach {
        if (it.value > 0 || !it.assets.isNullOrEmpty())
            retVal.addOutputsItem(it)
    }

    return retVal
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
