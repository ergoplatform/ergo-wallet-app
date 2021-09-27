package org.ergoplatform.android.transactions

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.utils.Base64Coder

private const val JSON_FIELD_TX = "reducedTx"
private const val JSON_FIELD_SENDER = "sender"
private const val JSON_FIELD_INPUTS = "inputs"

/**
 * Builds a cold signing request according to EIP19 with no size limitation
 */
fun buildColdSigningRequest(data: PromptSigningResult): String? {
    if (data.success) {
        val gson = Gson()
        val root = JsonObject()
        root.addProperty(JSON_FIELD_TX, String(Base64Coder.encode(data.serializedTx!!)))
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

fun parseColdSigningRequest(qrdata: String): PromptSigningResult {
    try {

        val jsonTree = JsonParser().parse(qrdata) as JsonObject

        // reducedTx is guaranteed
        val serializedTx = Base64Coder.decode(jsonTree.get(JSON_FIELD_TX).asString)

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

const val QR_SIZE_LIMIT = 3600
private const val QR_PREFIX_COLD_SIGNING_REQUEST = "CSR"
private const val QR_PREFIX_DATA_DELIMITER = '-'
private const val QR_PREFIX_HEADER_DELIMITER = '/'

fun coldSigninRequestToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String> {
    val actualSizeLimit = sizeLimit - 5 // reserve some space for our prefix

    if (serializedSigningRequest.length <= actualSizeLimit) {
        return listOf(QR_PREFIX_COLD_SIGNING_REQUEST + QR_PREFIX_DATA_DELIMITER + serializedSigningRequest)
    } else {
        val chunks = serializedSigningRequest.chunked(sizeLimit)
        var slice = 0
        return chunks.map {
            slice++
            QR_PREFIX_COLD_SIGNING_REQUEST + slice.toString() + QR_PREFIX_HEADER_DELIMITER + chunks.size + QR_PREFIX_DATA_DELIMITER + it
        }
    }
}

fun coldSigningRequestFromQrChunks(qrChunks: List<String>): PromptSigningResult {
    try {
        // check the list
        qrChunks.forEach {
            if (!isColdSigningRequestChunk(it))
                throw IllegalArgumentException("Not a cold signing request chunk")
        }
        val chunksSorted = qrChunks.sortedBy { getColdSigingRequestChunkIndex(it) }.map {
            it.substringAfter(
                QR_PREFIX_DATA_DELIMITER
            )
        }
        val qrdata = chunksSorted.joinToString("") { it }

        return parseColdSigningRequest(qrdata)

    } catch (t: Throwable) {
        return PromptSigningResult(false, errorMsg = t.message)
    }
}

fun isColdSigningRequestChunk(chunk: String): Boolean {
    return chunk.startsWith(QR_PREFIX_COLD_SIGNING_REQUEST) &&
            chunk.contains(QR_PREFIX_DATA_DELIMITER)
}

fun getColdSigingRequestChunkIndex(chunk: String): Int {
    val chunkWithoutHeaderPrefix = chunk.removePrefix(QR_PREFIX_COLD_SIGNING_REQUEST)
    val pages = chunkWithoutHeaderPrefix.substringBefore(QR_PREFIX_DATA_DELIMITER)
        .split(QR_PREFIX_HEADER_DELIMITER)
    return pages.firstOrNull()?.toIntOrNull() ?: 0
}