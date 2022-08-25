package org.ergoplatform.desktop.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import boofcv.alg.fiducial.qrcode.QrCodeEncoder
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage
import boofcv.kotlin.asBufferedImage

fun getQrCodeImageBitmap(text: String): ImageBitmap {
    val qr = QrCodeEncoder().addAutomatic(text).fixate()
    val generator = QrCodeGeneratorImage(15).render(qr)
    return generator.gray.asBufferedImage().toComposeImageBitmap()
}