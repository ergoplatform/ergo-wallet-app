package org.ergoplatform.transactions

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.deserializeErgobox
import org.ergoplatform.deserializeUnsignedTxOffline
import org.ergoplatform.utils.Base64Coder

private const val JSON_FIELD_REDUCED_TX = "reducedTx"
private const val JSON_FIELD_SIGNED_TX = "signedTx"
private const val JSON_FIELD_SENDER = "sender"
private const val JSON_FIELD_INPUTS = "inputs"

/**
 * Builds a cold signing request according to EIP19 with no size limitation
 */
fun buildColdSigningRequest(data: PromptSigningResult): String? {
    if (data.success) {
        val gson = GsonBuilder().disableHtmlEscaping().create()
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
        val gson = GsonBuilder().disableHtmlEscaping().create()
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
        val sender = jsonTree.get(JSON_FIELD_SENDER)?.asString

        val inputs = jsonTree.decodeBase64StringList(JSON_FIELD_INPUTS)

        return PromptSigningResult(true, serializedTx, inputs, sender)

    } catch (t: Throwable) {
        return PromptSigningResult(false, errorMsg = t.message)
    }
}

private fun JsonObject.decodeBase64StringList(fieldName: String): List<ByteArray>? {
    return get(fieldName)?.asJsonArray?.toList()?.map { Base64Coder.decode(it.asString) }
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

const val QR_DATA_LENGTH_LIMIT = 2000
const val QR_DATA_LENGTH_LOW_RES = 400
private const val QR_PROPERTY_COLD_SIGNING_REQUEST = "CSR"
private const val QR_PROPERTY_COLD_SIGNED_TX = "CSTX"
private const val QR_PROPERTY_INDEX = "p"
private const val QR_PROPERTY_PAGES = "n"

data class QrChunk(val index: Int, val pages: Int, val data: String)

fun coldSigningRequestToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String> {
    return buildQrChunks(QR_PROPERTY_COLD_SIGNING_REQUEST, sizeLimit, serializedSigningRequest)
}

fun coldSigningResponseToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String> {
    return buildQrChunks(QR_PROPERTY_COLD_SIGNED_TX, sizeLimit, serializedSigningRequest)
}

private fun buildQrChunks(
    prefix: String,
    sizeLimit: Int,
    serializedSigningRequest: String
): List<String> {
    val actualSizeLimit = sizeLimit - 30 - prefix.length // reserve some space for our prefix

    val gson = GsonBuilder().disableHtmlEscaping().create()

    if (serializedSigningRequest.length <= actualSizeLimit) {
        val root = JsonObject()
        root.addProperty(prefix, serializedSigningRequest)
        return listOf(gson.toJson(root))
    } else {
        val chunks = serializedSigningRequest.chunked(actualSizeLimit)
        var slice = 0
        return chunks.map {
            slice++
            val root = JsonObject()
            root.addProperty(prefix, it)
            root.addProperty(QR_PROPERTY_PAGES, chunks.size)
            root.addProperty(QR_PROPERTY_INDEX, slice)
            gson.toJson(root)
        }
    }
}

fun coldSigningRequestFromQrChunks(qrChunks: Collection<String>): PromptSigningResult {
    try {
        val qrdata = joinQrCodeChunks(qrChunks, QR_PROPERTY_COLD_SIGNING_REQUEST)

        return parseColdSigningRequest(qrdata)

    } catch (t: Throwable) {
        return PromptSigningResult(false, errorMsg = t.message)
    }
}

fun coldSigningResponseFromQrChunks(qrChunks: Collection<String>): SigningResult {
    try {
        val qrdata = joinQrCodeChunks(qrChunks, QR_PROPERTY_COLD_SIGNED_TX)

        return parseColdSigningResponse(qrdata)

    } catch (t: Throwable) {
        return SigningResult(false, errorMsg = t.message)
    }
}

private fun joinQrCodeChunks(qrChunks: Collection<String>, property: String): String {
    val qrList = qrChunks.map { parseQrChunk(it, property) }
    val chunksSorted = qrList.sortedBy { it.index }.map {
        if (it.pages != qrChunks.size)
            throw IllegalArgumentException("QR code chunk sizes differ")

        it.data
    }
    return chunksSorted.joinToString("") { it }
}

fun isColdSigningRequestChunk(chunk: String): Boolean {
    return getColdSigningRequestChunk(chunk) != null
}

fun getColdSigningRequestChunk(chunk: String): QrChunk? {
    return try {
        parseQrChunk(chunk, QR_PROPERTY_COLD_SIGNING_REQUEST)
    } catch (t: Throwable) {
        null
    }
}

fun getColdSignedTxChunk(chunk: String): QrChunk? {
    return try {
        parseQrChunk(chunk, QR_PROPERTY_COLD_SIGNED_TX)
    } catch (t: Throwable) {
        null
    }
}

private fun parseQrChunk(chunk: String, property: String): QrChunk {
    val jsonTree = JsonParser().parse(chunk) as JsonObject
    if (!jsonTree.has(property)) {
        throw java.lang.IllegalArgumentException("QR code does not contain element $property")
    }
    val index = jsonTree.get(QR_PROPERTY_INDEX)?.asInt ?: 1
    val pages = jsonTree.get(QR_PROPERTY_PAGES)?.asInt ?: 1
    return QrChunk(index, pages, jsonTree.get(property).asString)
}

fun PromptSigningResult.buildTransactionInfo(): TransactionInfo {
    val unsignedTx = deserializeUnsignedTxOffline(serializedTx!!)

    // deserialize input boxes and store in hashmap
    val inputBoxes = HashMap<String, TransactionInfoBox>()
    serializedInputs?.forEach { input ->
        try {
            val ergoBox = deserializeErgobox(input)
            ergoBox?.let {
                val inboxInfo = it.toTransactionInfoBox()
                inputBoxes.put(inboxInfo.boxId, inboxInfo)
            }
        } catch (t: Throwable) {
            // ignore errors
        }
    }

    return unsignedTx.buildTransactionInfo(inputBoxes)
}

class QrCodePagesCollector(private val parser: (String) -> QrChunk?) {
    private val qrCodeChunks = HashMap<Int, String>()
    var pagesCount = 0
    val pagesAdded get() = qrCodeChunks.size

    fun addPage(qrCodeChunk: String): Boolean {
        // returns false when QR code could not be not parsed
        val qrChunk = parser.invoke(qrCodeChunk) ?: return false

        val page = qrChunk.index
        val count = qrChunk.pages

        // returns false if new QR code has other pages count as the former ones, does not fit
        if (pagesCount != 0 && count != pagesCount) {
            return false
        }

        qrCodeChunks.put(page, qrCodeChunk)
        pagesCount = count

        return true
    }

    fun hasAllPages(): Boolean {
        return pagesAdded == pagesCount && pagesCount > 0
    }

    fun getAllPages(): Collection<String> {
        return qrCodeChunks.values
    }
}
