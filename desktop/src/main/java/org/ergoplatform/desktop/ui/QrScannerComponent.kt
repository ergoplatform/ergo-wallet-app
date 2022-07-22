package org.ergoplatform.desktop.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import boofcv.factory.fiducial.FactoryFiducial
import boofcv.io.webcamcapture.UtilWebcamCapture
import boofcv.kotlin.asGrayU8
import boofcv.struct.image.GrayU8
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.uilogic.STRING_LABEL_SCAN_QR
import org.ergoplatform.utils.LogUtils
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import kotlin.coroutines.coroutineContext

class QrScannerComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val callback: (String) -> Unit,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_LABEL_SCAN_QR)

    override val fullScreen: Boolean
        get() = true

    private suspend fun observeWebcam() {
        try {
            // Open a webcam and create the detector
            val webcam = UtilWebcamCapture.openDefault(800, 600)
            try {
                val detector = FactoryFiducial.qrcode(null, GrayU8::class.java)
                while (coroutineContext.isActive) {
                    // Load the image from the webcam
                    val image = webcam.image //?: break

                    // Convert to gray scale and detect QR codes inside
                    detector.process(image.asGrayU8())

                    for (qr in detector.detections) {
                        qrCodeScanned(qr.message)
                    }

                    // Update the display
                    imageState.value = image.toComposeImageBitmap()
                    delay(100)
                }
            } finally {
                webcam.close()
            }
        } catch (t: Throwable) {
            errorState.value = "Error accessing webcam"
            LogUtils.logDebug(this.javaClass.simpleName, t.message ?: "", t)
        }
    }

    /**
     * Get an image off the system clipboard.
     *
     * @return Returns an Image if successful; otherwise returns null.
     */
    private fun getImageFromClipboard(): BufferedImage? {
        val transferable: Transferable? =
            Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                return (transferable.getTransferData(DataFlavor.imageFlavor) as Image).toBufferedImage()
            } catch (t: Throwable) {
                LogUtils.logDebug(this.javaClass.simpleName, t.message ?: "", t)
            }
        }
        return null
    }

    private fun Image.toBufferedImage(): BufferedImage {
        if (this is BufferedImage) {
            return this
        }

        // Create a buffered image with transparency
        val bimage =
            BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)

        // Draw the image on to the buffered image
        val bGr = bimage.createGraphics()
        bGr.drawImage(this, 0, 0, null)
        bGr.dispose()

        // Return the buffered image
        return bimage
    }

    private fun qrCodeScanned(scannedQr: String) {
        navHost.router.pop()
        callback(scannedQr)
    }

    private val imageState = mutableStateOf<ImageBitmap?>(null)
    private val errorState = mutableStateOf("")

    init {
        componentScope().launch { observeWebcam() }
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        QrScannerScreen(
            imageState, errorState, ::qrCodeScanned, pasteImage = {
                val image = getImageFromClipboard()
                image?.let {
                    val detector = FactoryFiducial.qrcode(null, GrayU8::class.java)
                    // Convert to gray scale and detect QR codes inside
                    detector.process(image.asGrayU8())

                    val detections = detector.detections

                    if (detections.isNotEmpty())
                        qrCodeScanned(detections.first().message)
                    else
                        errorState.value =
                            "No QR code found in image. Make sure to copy QR code with a margin."
                } ?: run { errorState.value = "No image in clipboard" } // TODO i18N
            },
            dismiss = router::pop
        )
    }

}