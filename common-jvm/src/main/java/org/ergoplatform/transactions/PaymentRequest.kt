package org.ergoplatform

import java.net.URLDecoder
import java.net.URLEncoder

private const val PARAM_DELIMITER = "&"
private const val RECIPIENT_PARAM_PREFIX = "address="
private const val AMOUNT_PARAM_PREFIX = "amount="
private const val DESCRIPTION_PARAM_PREFIX = "description="
private const val URI_ENCODING = "utf-8"

/**
 * Token prefix is "token-<ErgoID>=", see https://github.com/ergoplatform/eips/blob/master/eip-0025.md#format
 */
private const val TOKEN_PARAM_PREFIX = "token-"

/**
 * referenced in AndroidManifest.xml
 */
private val explorerPaymentUrlPrefix
    get() =
        if (isErgoMainNet) "https://explorer.ergoplatform.com/payment-request?"
        else "https://testnet.ergoplatform.com/payment-request?"

/**
 * referenced in AndroidManifest.xml and Info.plist.xml
 */
private val PAYMENT_URI_SCHEME = "ergo:"

fun getExplorerPaymentRequestAddress(
    address: String,
    amount: Double = 0.0,
    description: String = ""
): String {
    return explorerPaymentUrlPrefix + RECIPIENT_PARAM_PREFIX +
            URLEncoder.encode(address, URI_ENCODING) +
            (if (amount > 0) PARAM_DELIMITER + AMOUNT_PARAM_PREFIX +
                    URLEncoder.encode(amount.toString(), URI_ENCODING) else "") +
            PARAM_DELIMITER + DESCRIPTION_PARAM_PREFIX +
            URLEncoder.encode(description, URI_ENCODING)
}

fun isPaymentRequestUrl(url: String): Boolean {
    return url.startsWith(explorerPaymentUrlPrefix, true) ||
            url.startsWith(PAYMENT_URI_SCHEME, true)
}

fun parsePaymentRequest(prString: String): PaymentRequest? {
    if (prString.startsWith(explorerPaymentUrlPrefix, true)) {
        // we have an explorer payment url
        val uriWithoutPrefix = prString.substring(explorerPaymentUrlPrefix.length)
        return parsePaymentRequestFromQuery(uriWithoutPrefix)
    } else if (prString.startsWith(PAYMENT_URI_SCHEME, true)) {
        // we have an uri scheme payment request: ergoplatform:address?moreParams
        val uriWithoutPrefix = prString.substring(PAYMENT_URI_SCHEME.length).trimStart('/')
        return parsePaymentRequestFromQuery(
            RECIPIENT_PARAM_PREFIX + uriWithoutPrefix.replaceFirst(
                '?',
                '&'
            )
        )
    } else if (isValidErgoAddress(prString)) {
        return PaymentRequest(prString)
    } else {
        return null
    }
}

private fun parsePaymentRequestFromQuery(query: String): PaymentRequest? {
    var address: String? = null
    var amount = ErgoAmount.ZERO
    var description = ""
    val tokenMap: HashMap<String, String> = HashMap()

    query.split('&').forEach {
        if (it.startsWith(RECIPIENT_PARAM_PREFIX)) {
            address =
                URLDecoder.decode(it.substring(RECIPIENT_PARAM_PREFIX.length), URI_ENCODING)
        } else if (it.startsWith(AMOUNT_PARAM_PREFIX)) {
            amount = URLDecoder.decode(it.substring(AMOUNT_PARAM_PREFIX.length), URI_ENCODING)
                .toErgoAmount() ?: ErgoAmount.ZERO
        } else if (it.startsWith(DESCRIPTION_PARAM_PREFIX)) {
            description =
                URLDecoder.decode(it.substring(DESCRIPTION_PARAM_PREFIX.length), URI_ENCODING)
        } else if (it.contains('=')) {
            // this could be a token
            // we accept token params without token-prefix to not break compatibility with
            // auction house for now.
            // TODO Q2/2022 remove backwards compatiblity
            val keyVal = it.split('=')
            try {
                val tokenId = keyVal.get(0)
                    .let { if (it.startsWith(TOKEN_PARAM_PREFIX)) it.substring(TOKEN_PARAM_PREFIX.length) else it }
                val tokenAmount = keyVal.get(1)
                // throws exception when it is not numeric
                tokenAmount.toDouble()
                tokenMap.put(tokenId, tokenAmount)
            } catch (t: Throwable) {
                // in this case, we haven't found a token :)
            }
        }
    }

    if (address != null) {
        return PaymentRequest(address!!, amount, description, tokenMap)
    } else {
        // no recipient, no sense
        return null
    }
}

data class PaymentRequest(
    val address: String,
    val amount: ErgoAmount = ErgoAmount.ZERO,
    val description: String = "",
    val tokens: HashMap<String, String> = HashMap(),
)

data class PaymentRequestWarning(
    val errorCode: String, val arguments: String? = null
)