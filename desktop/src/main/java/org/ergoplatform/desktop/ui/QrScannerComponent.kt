package org.ergoplatform.desktop.ui

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
import kotlin.coroutines.coroutineContext

class QrScannerComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val callback: (String) -> Unit,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_LABEL_SCAN_QR)

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

    private fun qrCodeScanned(scannedQr: String) {
        callback(scannedQr)
        navHost.router.pop()
    }

    private val imageState = mutableStateOf<ImageBitmap?>(null)
    private val errorState = mutableStateOf("")

    init {
        componentScope().launch { observeWebcam() }
    }

    @Composable
    override fun renderScreenContents() {
        QrScannerScreen(imageState, errorState, ::qrCodeScanned)
    }

}