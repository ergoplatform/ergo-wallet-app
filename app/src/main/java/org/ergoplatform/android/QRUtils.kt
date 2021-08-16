package org.ergoplatform.android

import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.net.URLDecoder
import java.net.URLEncoder

private val PAYMENT_URI_PREFIX = "https://explorer.ergoplatform.com/payment-request?"
private val PARAM_DELIMITER = "&"
private val RECIPIENT_PARAM_PREFIX = "address="
private val AMOUNT_PARAM_PREFIX = "amount="
private val DESCRIPTION_PARAM_PREFIX = "description="
private val URI_ENCODING = "utf-8"

fun setQrCodeToImageView(imageViewQrCode: ImageView, text: String, width: Int, height: Int) {
    try {
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(
            text,
            BarcodeFormat.QR_CODE,
            width,
            height
        )
        imageViewQrCode.setImageBitmap(bitmap)
    } catch (e: Exception) {
    }
}

fun getExplorerPaymentRequestAddress(
    address: String,
    amount: Float = 0f,
    description: String = ""
): String {
    return PAYMENT_URI_PREFIX + RECIPIENT_PARAM_PREFIX + URLEncoder.encode(address, URI_ENCODING) +
            PARAM_DELIMITER + AMOUNT_PARAM_PREFIX + URLEncoder.encode(
        amount.toString(),
        URI_ENCODING
    ) +
            PARAM_DELIMITER + DESCRIPTION_PARAM_PREFIX + URLEncoder.encode(
        description,
        URI_ENCODING
    )
}

fun parseContentFromQrCode(qrCode: String): QrCodeContent? {
    if (qrCode.startsWith(PAYMENT_URI_PREFIX, true)) {
        // we have a payment uri
        val uriWithoutPrefix = qrCode.substring(PAYMENT_URI_PREFIX.length)
        var address: String? = null
        var amount: Float = 0f
        var description: String = ""
        val tokenMap: HashMap<String, Double> = HashMap()

        uriWithoutPrefix.split('&').forEach {
            if (it.startsWith(RECIPIENT_PARAM_PREFIX)) {
                address =
                    URLDecoder.decode(it.substring(RECIPIENT_PARAM_PREFIX.length), URI_ENCODING)
            } else if (it.startsWith(AMOUNT_PARAM_PREFIX)) {
                amount = URLDecoder.decode(it.substring(AMOUNT_PARAM_PREFIX.length), URI_ENCODING)
                    .toFloatOrNull() ?: 0f
            } else if (it.startsWith(DESCRIPTION_PARAM_PREFIX)) {
                description =
                    URLDecoder.decode(it.substring(DESCRIPTION_PARAM_PREFIX.length), URI_ENCODING)
            } else if (it.contains('=')) {
                // this could be a token
                val keyVal = it.split("=")
                try {
                    val tokenId = keyVal.get(0)
                    val tokenAmount = keyVal.get(1).toDouble()
                    tokenMap.put(tokenId, tokenAmount)
                } catch (t: Throwable) {
                    // in this case, we haven't found a token :)
                }
            }
        }

        if (address != null) {
            return QrCodeContent(address!!, amount, description, tokenMap)
        } else {
            // no recipient, no sense
            return null
        }

    } else if (isValidErgoAddress(qrCode)) {
        return QrCodeContent(qrCode)
    } else {
        return null
    }
}

data class QrCodeContent(
    val address: String,
    val amount: Float = 0f,
    val description: String = "",
    val tokens: HashMap<String, Double> = HashMap(),
)