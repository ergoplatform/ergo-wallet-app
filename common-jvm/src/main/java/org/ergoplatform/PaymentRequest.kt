package org.ergoplatform

import java.net.URLDecoder
import java.net.URLEncoder

private val PARAM_DELIMITER = "&"
private val RECIPIENT_PARAM_PREFIX = "address="
private val AMOUNT_PARAM_PREFIX = "amount="
private val TOKEN_PARAM_PREFIX = "token-"
private val DESCRIPTION_PARAM_PREFIX = "description="
private val URI_ENCODING = "utf-8"

private val explorerPaymentUrlPrefix
    get() =
        if (isErgoMainNet) "https://explorer.ergoplatform.com/payment-request?"
        else "https://testnet.ergoplatform.com/payment-request?"

private val PAYMENT_URI_SCHEME = "ergoplatform:"

fun getExplorerPaymentRequestAddress(
    address: String,
    amount: Double = 0.0,
    description: String = ""
): String {
    return explorerPaymentUrlPrefix + RECIPIENT_PARAM_PREFIX + URLEncoder.encode(
        address,
        URI_ENCODING
    ) +
            PARAM_DELIMITER + AMOUNT_PARAM_PREFIX + URLEncoder.encode(
        amount.toString(),
        URI_ENCODING
    ) +
            PARAM_DELIMITER + DESCRIPTION_PARAM_PREFIX + URLEncoder.encode(
        description,
        URI_ENCODING
    )
}

fun isPaymentRequestUrl(url: String): Boolean {
    return url.startsWith(explorerPaymentUrlPrefix, true) || url.startsWith(
        PAYMENT_URI_SCHEME,
        true
    )
}

fun parsePaymentRequestFromQrCode(qrCode: String): PaymentRequest? {
    if (qrCode.startsWith(explorerPaymentUrlPrefix, true)) {
        // we have an explorer payment url
        val uriWithoutPrefix = qrCode.substring(explorerPaymentUrlPrefix.length)
        return parsePaymentRequestFromQuery(uriWithoutPrefix)
    } else if (qrCode.startsWith(PAYMENT_URI_SCHEME, true)) {
        // we have an uri scheme payment request: ergoplatform:address?moreParams
        val uriWithoutPrefix = qrCode.substring(PAYMENT_URI_SCHEME.length).trimStart('/')
        return parsePaymentRequestFromQuery(
            RECIPIENT_PARAM_PREFIX + uriWithoutPrefix.replaceFirst(
                '?',
                '&'
            )
        )
    } else if (isValidErgoAddress(qrCode)) {
        return PaymentRequest(qrCode)
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
            val keyVal = it.split('=')
            try {
                // we accept token params without token-prefix to not break compatibility with
                // auction house for now. Can be removed later
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