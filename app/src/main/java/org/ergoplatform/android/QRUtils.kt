package org.ergoplatform.android

import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.net.URLEncoder

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
    val encoding = "utf-8"
    return "https://explorer.ergoplatform.com/payment-request?address=" +
            URLEncoder.encode(address, encoding) +
            "&amount=" + URLEncoder.encode(amount.toString(), encoding) + "&description=" +
            URLEncoder.encode(
                description, encoding
            )
}

