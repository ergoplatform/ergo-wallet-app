package org.ergoplatform.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.api.ErgoExplorerApi
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.BoxAttachmentBuilder
import org.ergoplatform.appkit.impl.Eip4TokenBuilder
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.InputInfo
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.getErgoNetworkType
import org.ergoplatform.persistance.WalletToken
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
 * value of nanoergs and list of tokens the box holds or will hold (with EIP4 information).
 */
data class TransactionInfoBox(
    val boxId: String,
    val address: String,
    val value: Long,
    val tokens: List<AssetInstanceInfo>
)

/**
 * builds transaction info from Ergo Pay Signing Request, fetches necessary boxes data
 * use within an applicable try/catch phrase
 */
suspend fun Transaction.buildTransactionInfo(ergoApiService: ErgoExplorerApi): TransactionInfo {
    return withContext(Dispatchers.IO) {
        val inputsMap = HashMap<String, TransactionInfoBox>()
        inputBoxesIds.forEach { boxId ->
            val boxInfo = ergoApiService.getBoxInformation(boxId).execute().body()
                ?: ergoApiService.getUnconfirmedBoxById(boxId)

            if (boxInfo == null)
                throw IllegalStateException("Could not retrieve information for box $boxId.\n" +
                        "If you have unconfirmed transactions, try again after confirmation.")
            else
                inputsMap[boxInfo.boxId] = boxInfo.toTransactionInfoBox()
        }
        buildTransactionInfo(inputsMap)
    }
}

/**
 * @return TransactionInfo data class for this Transaction. The TransactionInfo
 *         holds information for the boxes to spend
 *         and more information for tokens on the boxes to issue.
 */
fun Transaction.buildTransactionInfo(inputBoxes: HashMap<String, TransactionInfoBox>): TransactionInfo {
    return buildTransactionInfo(
        inputBoxes,
        inputBoxesIds,
        outputs,
        id
    )
}

fun UnsignedTransaction.buildTransactionInfo(tokens: List<WalletToken>?): TransactionInfo {
    val inputBoxes = HashMap<String, TransactionInfoBox>()
    inputs.forEach { input ->
        val inboxInfo = input.toTransactionInfoBox()
        // use wallet tokens to set decimal and name information of tokens sent
        tokens?.let {
            inboxInfo.tokens.forEach { assetInfo ->
                tokens.firstOrNull { it.tokenId == assetInfo.tokenId }?.let { walletToken ->
                    assetInfo.name = walletToken.name
                    assetInfo.decimals = walletToken.decimals
                }
            }
        }
        inputBoxes[inboxInfo.boxId] = inboxInfo
    }
    return buildTransactionInfo(
        inputBoxes,
        inputBoxesIds,
        outputs,
        id
    )
}

private fun buildTransactionInfo(
    inputBoxes: HashMap<String, TransactionInfoBox>,
    inputBoxesIds: List<String>,
    outboxes: List<TransactionBox>,
    txId: String,
): TransactionInfo {
    val inputsList = ArrayList<InputInfo>()
    val outputsList = ArrayList<OutputInfo>()
    val tokensMap = HashMap<String, AssetInstanceInfo>()

    // now add to TransactionInfo, if possible
    inputBoxesIds.forEach { boxid ->
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

    outboxes.forEach { transactionBox ->
        val outputInfo = OutputInfo()

        outputInfo.address =
            Address.fromErgoTree(transactionBox.ergoTree, getErgoNetworkType()).toString()
        outputInfo.value = transactionBox.value

        outputsList.add(outputInfo)

        getAssetInstanceInfosFromErgoBoxToken(transactionBox.tokens).forEach {
            outputInfo.addAssetsItem(it)

            // if we know about the token from the inboxes, add more information
            tokensMap.get(it.tokenId)?.let { tokenInfo ->
                it.name = tokenInfo.name
                it.decimals = tokenInfo.decimals
            } ?: if (it.tokenId.equals(inputsList[0].boxId)) {
                // could be minted right here, check outbox info
                val token = try {
                    Eip4TokenBuilder.buildFromErgoBox(
                        it.tokenId,
                        transactionBox
                    )
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

    return TransactionInfo(txId, inputsList, outputsList)
}

/**
 * @return TransactionInfoBox, which is more Kotlin-friendly to work with
 */
fun InputBox.toTransactionInfoBox(): TransactionInfoBox {
    return TransactionInfoBox(
        id.toString(),
        Address.fromErgoTree(ergoTree, getErgoNetworkType()).toString(),
        value,
        getAssetInstanceInfosFromErgoBoxToken(tokens)
    )
}

/**
 * @return TransactionInfoBox, which is more Kotlin-friendly to work with
 */
fun OutputInfo.toTransactionInfoBox(): TransactionInfoBox {
    return TransactionInfoBox(boxId, address, value, assets)
}

private fun getAssetInstanceInfosFromErgoBoxToken(tokens: List<ErgoToken>): List<AssetInstanceInfo> {
    return tokens.map {
        val tokenInfo = AssetInstanceInfo()
        tokenInfo.amount = it.value
        tokenInfo.tokenId = it.id.toString()

        tokenInfo
    }
}

/**
 * combines inboxes and outboxes and reduces change amounts for user-friendly outputs
 * returns a new object with less information, only a single entry for each participating address
 * either in inputs or in outputs
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
                output.assets?.firstOrNull { inputAssetInfo.tokenId.equals(it.tokenId) }
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

fun OutputInfo.getAttachmentText(): String? = additionalRegisters?.let { registers ->
    (BoxAttachmentBuilder.buildFromAdditionalRegisters(registers) as? BoxAttachmentPlainText)?.text
}