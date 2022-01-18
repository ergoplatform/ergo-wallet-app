package org.ergoplatform

import com.google.gson.JsonParser
import org.ergoplatform.transactions.*
import org.ergoplatform.utils.Base64Coder
import org.ergoplatform.utils.fetchHttpGetStringSync
import org.ergoplatform.utils.isLocalOrIpAddress

/**
 * EIP-0020 ErgoPaySigningRequest
 * everything is optional, but it should have either reducedTx or message
 */
data class ErgoPaySigningRequest(
    val reducedTx: ByteArray?,
    val p2pkAddress: String? = null,
    val message: String? = null,
    val messageSeverity: MessageSeverity = MessageSeverity.NONE,
    val replyToUrl: String? = null
)

enum class MessageSeverity { NONE, INFORMATION, WARNING, ERROR }

private const val uriSchemePrefix = "ergopay:"

fun isErgoPaySigningRequest(uri: String): Boolean {
    return uri.startsWith(uriSchemePrefix, true)
}

/**
 * gets Ergo Pay Signing Request from Ergo Pay URI. If this is not a static request, this will
 * do a network request, so call this only from non-UI thread and within an applicable try/catch
 * phrase
 */
fun getErgoPaySigningRequest(requestData: String): ErgoPaySigningRequest {
    if (!isErgoPaySigningRequest(requestData)) {
        throw IllegalArgumentException("No ergopay URI provided.")
    }

    // static request?
    if (isErgoPayStaticRequest(requestData)) {
        return parseErgoPaySigningRequestFromUri(requestData)
    } else {
        // TODO Ergo Pay check for address placeholder

        // use http for development purposes
        val httpUrl = (if (isLocalOrIpAddress(requestData)) "http:" else "https:") +
                requestData.substringAfter(uriSchemePrefix)

        val jsonResponse = fetchHttpGetStringSync(httpUrl)
        return parseErgoPaySigningRequestFromJson(jsonResponse)
    }
}

private const val JSON_KEY_REDUCED_TX = "reducedTx"
private const val JSON_KEY_ADDRESS = "address"
private const val JSON_KEY_REPLY_TO = "replyTo"
private const val JSON_KEY_MESSAGE = "message"
private const val JSON_KEY_MESSAGE_SEVERITY = "messageSeverity"

fun parseErgoPaySigningRequestFromJson(jsonString: String): ErgoPaySigningRequest {
    val jsonObject = JsonParser().parse(jsonString).asJsonObject
    val reducedTx = jsonObject.get(JSON_KEY_REDUCED_TX)?.asString?.let {
        Base64Coder.decode(it, true)
    }

    return ErgoPaySigningRequest(
        reducedTx, jsonObject.get(JSON_KEY_ADDRESS)?.asString,
        jsonObject.get(JSON_KEY_MESSAGE)?.asString,
        jsonObject.get(JSON_KEY_MESSAGE_SEVERITY)?.asString?.let { MessageSeverity.valueOf(it) }
            ?: MessageSeverity.NONE,
        jsonObject.get(JSON_KEY_REPLY_TO)?.asString
    )
}

fun isErgoPayStaticRequest(requestData: String) =
    isErgoPaySigningRequest(requestData) && !requestData.startsWith("$uriSchemePrefix//", true)

private fun parseErgoPaySigningRequestFromUri(uri: String): ErgoPaySigningRequest {
    val uriWithoutPrefix = uri.substring(uriSchemePrefix.length)
    val reducedTx = Base64Coder.decode(uriWithoutPrefix, true)

    return ErgoPaySigningRequest(reducedTx)
}

/**
 * builds transaction info from Ergo Pay Signing Request, fetches necessary boxes data
 * call this only from non-UI thread and within an applicable try/catch phrase
 */
fun ErgoPaySigningRequest.buildTransactionInfo(ergoApiService: ErgoApiService): TransactionInfo {
    val unsignedTx = deserializeUnsignedTxOffline(reducedTx!!)

    val inputsMap = HashMap<String, TransactionInfoBox>()
    unsignedTx.getInputBoxesIds().forEach {
        val boxInfo = ergoApiService.getBoxInformation(it).execute().body()!!
        inputsMap.put(boxInfo.boxId, boxInfo.toTransactionInfoBox())
    }

    // TODO Ergo Pay when minting new tokens, check if information about name and decimals can be obtianed

    return unsignedTx.buildTransactionInfo(inputsMap)
}

