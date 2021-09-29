package org.ergoplatform.transactions

import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.InputInfo
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.explorer.client.model.TransactionInfo
import kotlin.math.min

/**
 * combines inboxes and outboxes and reduces change amounts for user-friendly outputs
 * returns a new object with less information
 */
fun TransactionInfo.reduceBoxes(): TransactionInfo {
    val retVal = TransactionInfo()

    retVal.id = id

    // combine boxes to or from same addresses
    val combinedInputs = HashMap<String, InputInfo>()

    inputs.forEach {
        val combined = combinedInputs.get(it.address ?: it.boxId) ?: InputInfo()
        combined.address = it.address
        combined.boxId = it.boxId
        combined.value = (combined.value ?: 0) + it.value
        it.assets?.forEach { combined.addAssetsItem(it) }
        combinedInputs.put(it.address ?: it.boxId, combined)
    }

    val combinedOutputs = HashMap<String, OutputInfo>()

    outputs.forEach {
        val combined = combinedOutputs.get(it.address ?: it.boxId) ?: OutputInfo()
        combined.address = it.address
        combined.boxId = it.boxId
        combined.value = (combined.value ?: 0) + it.value
        it.assets?.forEach { combined.addAssetsItem(it) }
        combinedOutputs.put(it.address ?: it.boxId, combined)
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

            // TODO reduce token amounts
        }
    }

    combinedInputs.values.forEach {
        if (it.value > 0 || !it.assets.isNullOrEmpty())
            retVal.addInputsItem(it)
    }
    combinedOutputs.values.forEach {
        if (it.value > 0 || !it.assets.isNullOrEmpty())
            retVal.addOutputsItem(it)
    }

    return retVal
}

fun combineTokens(tokens: List<AssetInstanceInfo>): List<AssetInstanceInfo> {
    // combine tokens with same id but different list entries
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
