package org.ergoplatform.transactions

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.deserializeUnsignedTxOffline
import org.ergoplatform.utils.*

/**
 * EIP-0020 ErgoPaySigningRequest
 * everything is optional, but it should have either reducedTx or message
 */
data class ErgoPaySigningRequest(
    val reducedTx: ByteArray?,
    val addressesToUse: List<String> = emptyList(),
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
    p2pkAddressList: List<String> = emptyList()
): ErgoPaySigningRequest {
    if (!isErgoPaySigningRequest(requestData)) {
        throw IllegalArgumentException("No ergopay URI provided.")
    }


    val epsr = if (isErgoPayStaticRequest(requestData)) {
        // static request
        parseErgoPaySigningRequestFromUri(requestData)
    } else {
        // dynamic request
        val addressRequest = isErgoPayDynamicWithAddressRequest(requestData)
        val multipleAddresses = addressRequest && p2pkAddressList.size > 1
        val ergopayUrl = if (addressRequest) {
            if (p2pkAddressList.isEmpty())
                throw IllegalArgumentException("Ergo Pay address request, but no address given")

            ergoPayAddressRequestSetAddress(
                requestData,
                if (multipleAddresses) URL_CONST_MULTIPLE_ADDRESSES else p2pkAddressList.first()
            )
        } else requestData

        // use http for development purposes
        val httpUrl = ergoPayUrlToHttpUrl(ergopayUrl)

        val jsonResponse =
            if (multipleAddresses) {
                val jsonBody = p2pkAddressList.map { "\"$it\"" }.joinToString(",", "[", "]")
                httpPostStringSync(httpUrl, jsonBody, timeout = 30, headers = getErgoPayHeaders())
            } else
                fetchHttpGetStringSync(httpUrl, timeout = 30, headers = getErgoPayHeaders())
        parseErgoPaySigningRequestFromJson(jsonResponse)
    }

    return epsr
}

private fun ergoPayUrlToHttpUrl(ergopayUrl: String) =
    (if (isLocalOrIpAddress(ergopayUrl)) "http:" else "https:") +
            ergopayUrl.substringAfter(uriSchemePrefix)

private fun ergoPayAddressRequestSetAddress(requestData: String, p2pkAddress: String) =
    requestData.replace(placeHolderP2Pk, p2pkAddress)
        .replace(urlEncodedPlaceHolderP2Pk, p2pkAddress)

private const val JSON_KEY_REDUCED_TX = "reducedTx"
private const val JSON_KEY_ADDRESS = "address"
private const val JSON_KEY_ADDRESSES = "addresses"
private const val JSON_KEY_REPLY_TO = "replyTo"
private const val JSON_KEY_MESSAGE = "message"
private const val JSON_KEY_MESSAGE_SEVERITY = "messageSeverity"

private const val HEADER_KEY_MULTIPLE_TX = "ErgoPay-MultipleTx"
private const val HEADER_KEY_MULTIPLE_ADDRESSES = "ErgoPay-CanSelectMultipleAddresses"
private const val HEADER_VALUE_SUPPORTED = "supported"
private const val URL_CONST_MULTIPLE_ADDRESSES_CHECK = "multiple_check"
private const val URL_CONST_MULTIPLE_ADDRESSES = "multiple"

/**
 * @return ErgoPaySigningRequest parsed from `jsonString`, may throw Exceptions
 */
fun parseErgoPaySigningRequestFromJson(jsonString: String): ErgoPaySigningRequest {
    val jsonObject = JsonParser().parse(jsonString).asJsonObject
    val reducedTx = jsonObject.get(JSON_KEY_REDUCED_TX)?.asString?.let {
        Base64Coder.decode(it, true)
    }

    val addressesList = if (jsonObject.has(JSON_KEY_ADDRESSES)) {
        jsonObject.get(JSON_KEY_ADDRESSES).asJsonArray.toList()
    } else jsonObject.get(JSON_KEY_ADDRESS)?.let { listOf(it) }

    return ErgoPaySigningRequest(
        reducedTx, addressesList?.map { it.asString } ?: emptyList(),
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

/**
 * @return true if given ergoPayUrl is a dynamic ergopay url with address request and the dApp
 *          can handle multiple addresses with a post request.
 */
fun canErgoPayAddressRequestHandleMultiple(ergoPayUrl: String): Boolean {
    return if (!isErgoPayDynamicWithAddressRequest(ergoPayUrl))
        false
    else {
        val checkMultipleUrl =
            ergoPayAddressRequestSetAddress(ergoPayUrl, URL_CONST_MULTIPLE_ADDRESSES_CHECK)
        try {
            httpPostStringSync(
                ergoPayUrlToHttpUrl(checkMultipleUrl),
                "",
                headers = getErgoPayHeaders()
            )
            LogUtils.logDebug(
                "ErgoPay",
                "canErgoPayAddressRequestHandleMultiple to $checkMultipleUrl succeeded"
            )
            true
        } catch (t: Throwable) {
            LogUtils.logDebug(
                "ErgoPay",
                "canErgoPayAddressRequestHandleMultiple to $checkMultipleUrl resolved to false",
                t
            )
            false
        }
    }
}

private fun parseErgoPaySigningRequestFromUri(uri: String): ErgoPaySigningRequest {
    val uriWithoutPrefix = uri.substring(uriSchemePrefix.length)
    val reducedTx = Base64Coder.decode(uriWithoutPrefix, true)

    return ErgoPaySigningRequest(reducedTx)
}

private fun getErgoPayHeaders() = mapOf(
    HEADER_KEY_MULTIPLE_ADDRESSES to HEADER_VALUE_SUPPORTED,
)

/**
 * builds transaction info from Ergo Pay Signing Request, fetches necessary boxes data
 * switches to non-UI thread, use within an applicable try/catch phrase
 */
suspend fun ErgoPaySigningRequest.buildTransactionInfo(ergoApiService: ApiServiceManager): TransactionInfo? {
    if (reducedTx == null) return null
    val reducedTransaction = deserializeUnsignedTxOffline(reducedTx)
    return reducedTransaction.buildTransactionInfo(ergoApiService)
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