package org.ergoplatform.transactions

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.ErgoApi
import org.ergoplatform.deserializeUnsignedTxOffline
import org.ergoplatform.utils.*

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
) {
    init {
        require(message != null || reducedTx != null) {
            "Ergo Pay Signing Request should contain at least message or reducedTx"
        }
    }
}

/**
 * MessageSeverity for showing a symbol next to a ErgoPaySigningRequest#message
 */
enum class MessageSeverity { NONE, INFORMATION, WARNING, ERROR }

private const val uriSchemePrefix = "ergopay:"
private const val placeHolderP2Pk = "#P2PK_ADDRESS#"
private const val urlEncodedPlaceHolderP2Pk = "#P2PK_ADDRESS%23"

/**
 * @return true if the given `uri` is a ErgoPay signing request URI
 */
fun isErgoPaySigningRequest(uri: String): Boolean {
    return uri.startsWith(uriSchemePrefix, true)
}

/**
 * gets Ergo Pay Signing Request from Ergo Pay URI. If this is not a static request, this will
 * do a network request, so call this only from non-UI thread and within an applicable try/catch
 * phrase
 */
fun getErgoPaySigningRequest(
    requestData: String,
    p2pkAddress: String? = null
): ErgoPaySigningRequest {
    if (!isErgoPaySigningRequest(requestData)) {
        throw IllegalArgumentException("No ergopay URI provided.")
    }

    // static request?
    val epsr = if (isErgoPayStaticRequest(requestData)) {
        parseErgoPaySigningRequestFromUri(requestData)
    } else {
        val ergopayUrl = if (isErgoPayDynamicWithAddressRequest(requestData)) {
            p2pkAddress?.let {
                requestData.replace(placeHolderP2Pk, p2pkAddress)
                    .replace(urlEncodedPlaceHolderP2Pk, p2pkAddress)
            } ?: throw IllegalArgumentException("Ergo Pay address request, but no address given")
        } else requestData

        // use http for development purposes
        val httpUrl = (if (isLocalOrIpAddress(requestData)) "http:" else "https:") +
                ergopayUrl.substringAfter(uriSchemePrefix)

        val jsonResponse = fetchHttpGetStringSync(httpUrl)
        parseErgoPaySigningRequestFromJson(jsonResponse)
    }

    return epsr
}

private const val JSON_KEY_REDUCED_TX = "reducedTx"
private const val JSON_KEY_ADDRESS = "address"
private const val JSON_KEY_REPLY_TO = "replyTo"
private const val JSON_KEY_MESSAGE = "message"
private const val JSON_KEY_MESSAGE_SEVERITY = "messageSeverity"

/**
 * @return ErgoPaySigningRequest parsed from `jsonString`, may throw Exceptions
 */
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

/**
 * @return true if `requestData` is a static ErgoPay signing request holding a reduced transaction
 */
fun isErgoPayStaticRequest(requestData: String) =
    isErgoPaySigningRequest(requestData) && !isErgoPayDynamicRequest(requestData)

/**
 * @return true if `requestData` is a dynamic ErgoPay signing request, holding an URL to fetch
 *         the actual signing request data from
 */
fun isErgoPayDynamicRequest(requestData: String) =
    requestData.startsWith("$uriSchemePrefix//", true)

/**
 * @return true if `requestData` is a dynamic ErgoPay signing request, holding an URL to fetch
 *         the actual signing request data from, and the URL contains a placeholder to fill with
 *         the user's p2pk address
 */
fun isErgoPayDynamicWithAddressRequest(requestData: String) =
    isErgoPayDynamicRequest(requestData) &&
            (requestData.contains(placeHolderP2Pk) || requestData.contains(urlEncodedPlaceHolderP2Pk))

private fun parseErgoPaySigningRequestFromUri(uri: String): ErgoPaySigningRequest {
    val uriWithoutPrefix = uri.substring(uriSchemePrefix.length)
    val reducedTx = Base64Coder.decode(uriWithoutPrefix, true)

    return ErgoPaySigningRequest(reducedTx)
}

/**
 * builds transaction info from Ergo Pay Signing Request, fetches necessary boxes data
 * call this only from non-UI thread and within an applicable try/catch phrase
 */
fun ErgoPaySigningRequest.buildTransactionInfo(ergoApiService: ErgoApi): TransactionInfo? {
    if (reducedTx == null) return null

    val unsignedTx = deserializeUnsignedTxOffline(reducedTx)

    val inputsMap = HashMap<String, TransactionInfoBox>()
    unsignedTx.getInputBoxesIds().forEach {
        val boxInfo = ergoApiService.getBoxInformation(it).execute().body()!!
        inputsMap.put(boxInfo.boxId, boxInfo.toTransactionInfoBox())
    }

    return unsignedTx.buildTransactionInfo(inputsMap)
}

private const val JSON_FIELD_TX_ID = "txId"

/**
 * Sends a reply to dApp, if necessary. Will make a https request to dApp
 * Call this only from non-UI thread and within an applicable try/catch phrase
 */
fun ErgoPaySigningRequest.sendReplyToDApp(txId: String) {
    replyToUrl?.let {
        val jsonString = run {
            val gson = GsonBuilder().disableHtmlEscaping().create()
            val root = JsonObject()
            root.addProperty(JSON_FIELD_TX_ID, txId)
            gson.toJson(root)
        }

        httpPostStringSync(it, jsonString, MEDIA_TYPE_JSON)
    }
}