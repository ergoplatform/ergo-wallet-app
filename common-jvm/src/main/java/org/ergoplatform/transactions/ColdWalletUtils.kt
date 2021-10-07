package org.ergoplatform.android.transactions

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.ErgoBox
import org.ergoplatform.android.deserializeErgobox
import org.ergoplatform.android.deserializeUnsignedTx
import org.ergoplatform.android.ergoNetworkType
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.ErgoId
import org.ergoplatform.appkit.Iso
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.InputInfo
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.utils.Base64Coder
import scala.Tuple2
import scala.collection.JavaConversions
import special.collection.Coll

private const val JSON_FIELD_REDUCED_TX = "reducedTx"
private const val JSON_FIELD_SIGNED_TX = "signedTx"
private const val JSON_FIELD_SENDER = "sender"
private const val JSON_FIELD_INPUTS = "inputs"

/**
 * Builds a cold signing request according to EIP19 with no size limitation
 */
fun buildColdSigningRequest(data: PromptSigningResult): String? {
    if (data.success) {
        val gson = Gson()
        val root = JsonObject()
        root.addProperty(JSON_FIELD_REDUCED_TX, String(Base64Coder.encode(data.serializedTx!!)))
        root.addProperty(JSON_FIELD_SENDER, data.address)
        val inputsarray = JsonArray()
        data.serializedInputs?.map { String(Base64Coder.encode(it)) }
            ?.forEach { inputsarray.add(it) }
        root.add(JSON_FIELD_INPUTS, inputsarray)
        return gson.toJson(root)

    } else {
        return null
    }
}

fun buildColdSigningResponse(data: SigningResult): String? {
    if (data.success) {
        val gson = Gson()
        val root = JsonObject()
        root.addProperty(JSON_FIELD_SIGNED_TX, String(Base64Coder.encode(data.serializedTx!!)))
        return gson.toJson(root)

    } else {
        return null
    }
}

fun parseColdSigningRequest(qrData: String): PromptSigningResult {
    try {

        val jsonTree = JsonParser().parse(qrData) as JsonObject

        // reducedTx is guaranteed
        val serializedTx = Base64Coder.decode(jsonTree.get(JSON_FIELD_REDUCED_TX).asString)

        // sender is optional
        val sender =
            if (jsonTree.has(JSON_FIELD_SENDER)) jsonTree.get(JSON_FIELD_SENDER).asString else null

        val inputs = if (jsonTree.has(JSON_FIELD_INPUTS)) {
            jsonTree.get(JSON_FIELD_INPUTS).asJsonArray.toList()
                .map { Base64Coder.decode(it.asString) }
        } else null

        return PromptSigningResult(true, serializedTx, inputs, sender)

    } catch (t: Throwable) {
        return PromptSigningResult(false, errorMsg = t.message)
    }
}

fun parseColdSigningResponse(qrData: String): SigningResult {
    try {

        val jsonTree = JsonParser().parse(qrData) as JsonObject

        // reducedTx is guaranteed
        val serializedTx = Base64Coder.decode(jsonTree.get(JSON_FIELD_SIGNED_TX).asString)

        return SigningResult(true, serializedTx)

    } catch (t: Throwable) {
        return SigningResult(false, errorMsg = t.message)
    }
}

const val QR_SIZE_LIMIT = 2900
private const val QR_PREFIX_COLD_SIGNING_REQUEST = "CSR"
private const val QR_PREFIX_COLD_SIGNED_TX = "CSTX"
private const val QR_PREFIX_DATA_DELIMITER = '-'
private const val QR_PREFIX_HEADER_DELIMITER = '/'

fun coldSigninRequestToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String> {
    return buildQrChunks(QR_PREFIX_COLD_SIGNING_REQUEST, sizeLimit, serializedSigningRequest)
}

fun coldSigningResponseToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String> {
    return buildQrChunks(QR_PREFIX_COLD_SIGNED_TX, sizeLimit, serializedSigningRequest)
}

private fun buildQrChunks(
    prefix: String,
    sizeLimit: Int,
    serializedSigningRequest: String
): List<String> {
    val actualSizeLimit = sizeLimit - 20 // reserve some space for our prefix

    if (serializedSigningRequest.length <= actualSizeLimit) {
        return listOf(prefix + QR_PREFIX_DATA_DELIMITER + serializedSigningRequest)
    } else {
        val chunks = serializedSigningRequest.chunked(sizeLimit)
        var slice = 0
        return chunks.map {
            slice++
            prefix + QR_PREFIX_HEADER_DELIMITER + slice.toString() + QR_PREFIX_HEADER_DELIMITER + chunks.size + QR_PREFIX_DATA_DELIMITER + it
        }
    }
}

fun coldSigningRequestFromQrChunks(qrChunks: Collection<String>): PromptSigningResult {
    try {
        // check the list
        qrChunks.forEach {
            if (!isColdSigningRequestChunk(it))
                throw IllegalArgumentException("Not a cold signing request chunk")
        }
        val qrdata = joinQrCodeChunks(qrChunks)

        return parseColdSigningRequest(qrdata)

    } catch (t: Throwable) {
        return PromptSigningResult(false, errorMsg = t.message)
    }
}

fun coldSigningResponseFromQrChunks(qrChunks: Collection<String>): SigningResult {
    try {
        qrChunks.forEach {
            if (!isColdSignedTxChunk(it))
                throw IllegalArgumentException("Not a cold signing request chunk")
        }
        val qrdata = joinQrCodeChunks(qrChunks)

        return parseColdSigningResponse(qrdata)

    } catch (t: Throwable) {
        return SigningResult(false, errorMsg = t.message)
    }
}

private fun joinQrCodeChunks(qrChunks: Collection<String>): String {
    val chunksSorted = qrChunks.sortedBy { getQrChunkIndex(it) }.map {
        if (getQrChunkPagesCount(it) != qrChunks.size)
            throw IllegalArgumentException("QR code chunk sizes differ")

        it.substringAfter(
            QR_PREFIX_DATA_DELIMITER
        )
    }
    val qrdata = chunksSorted.joinToString("") { it }
    return qrdata
}

fun isColdSigningRequestChunk(chunk: String): Boolean {
    return chunk.startsWith(QR_PREFIX_COLD_SIGNING_REQUEST) &&
            chunk.contains(QR_PREFIX_DATA_DELIMITER)
}

fun isColdSignedTxChunk(chunk: String): Boolean {
    return chunk.startsWith(QR_PREFIX_COLD_SIGNED_TX) &&
            chunk.contains(QR_PREFIX_DATA_DELIMITER)
}

fun getQrChunkIndex(chunk: String): Int {
    val chunkWithoutData = chunk.substringBefore(QR_PREFIX_DATA_DELIMITER)
    if (!chunkWithoutData.contains(QR_PREFIX_HEADER_DELIMITER))
        return 0

    val chunkWithoutHeaderPrefix = chunk.substringAfter(QR_PREFIX_HEADER_DELIMITER)
    val pages = chunkWithoutHeaderPrefix.substringBefore(QR_PREFIX_DATA_DELIMITER)
        .split(QR_PREFIX_HEADER_DELIMITER)
    return pages.firstOrNull()?.toIntOrNull() ?: 0
}

fun getQrChunkPagesCount(chunk: String): Int {
    val chunkWithoutData = chunk.substringBefore(QR_PREFIX_DATA_DELIMITER)
    if (!chunkWithoutData.contains(QR_PREFIX_HEADER_DELIMITER))
        return 1

    val pages = chunkWithoutData.substringBefore(QR_PREFIX_DATA_DELIMITER)
        .split(QR_PREFIX_HEADER_DELIMITER)
    return pages.lastOrNull()?.toIntOrNull() ?: 1
}

/*
 * TODO #13 use own db entity data class (null safe)
 */
fun buildTransactionInfoFromReduced(
    serializedTx: ByteArray,
    serializedInputs: List<ByteArray>? = null
): TransactionInfo {
    val unsignedTx = deserializeUnsignedTx(serializedTx)

    val retVal = TransactionInfo()

    retVal.id = unsignedTx.id()

    // deserialize input boxes and store in hashmap
    val inputBoxes = HashMap<String, ErgoBox>()
    serializedInputs?.forEach { input ->
        try {
            val ergoBox = deserializeErgobox(input)
            ergoBox?.let {
                inputBoxes.put(ErgoId(ergoBox.id()).toString(), ergoBox)
            }
        } catch (t: Throwable) {
            // ignore errors
        }
    }

    // now add to TransactionInfo, if possible
    JavaConversions.seqAsJavaList(unsignedTx.inputs())!!.forEach {
        val boxid = ErgoId(it.boxId()).toString()
        val inputInfo = InputInfo()
        inputInfo.boxId = boxid
        inputBoxes.get(boxid)?.let {
            inputInfo.address = Address.fromErgoTree(it.ergoTree(), ergoNetworkType).toString()
            inputInfo.value = it.value()
            getAssetInstanceInfos(it.additionalTokens()).forEach {
                inputInfo.addAssetsItem(it)
            }
        }
        retVal.addInputsItem(inputInfo)
    }

    JavaConversions.seqAsJavaList(unsignedTx.outputCandidates())!!.forEach {
        val outputInfo = OutputInfo()

        outputInfo.address = Address.fromErgoTree(it.ergoTree(), ergoNetworkType).toString()
        outputInfo.value = it.value()

        retVal.addOutputsItem(outputInfo)

        getAssetInstanceInfos(it.additionalTokens()).forEach {
            outputInfo.addAssetsItem(it)
        }
    }

    return retVal

}

private fun getAssetInstanceInfos(tokens: Coll<Tuple2<ByteArray, Any>>): List<AssetInstanceInfo> {
    val tokens = Iso.isoTokensListToPairsColl().from(tokens)
    return tokens.map {
        val tokenInfo = AssetInstanceInfo()
        tokenInfo.amount = it.value
        tokenInfo.tokenId = it.id.toString()

        tokenInfo
    }
}

