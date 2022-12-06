package org.ergoplatform.ios.ui

import org.ergoplatform.uilogic.STRING_ERROR_CAMERA
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.audiotoolbox.AudioServices
import org.robovm.apple.avfoundation.*
import org.robovm.apple.dispatch.DispatchQueue
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

/**
 * Shows a view for scanning QR codes
 *
 * @param qrCodeScannedListener invoked when QR code is scanned
 * @param dismissAnimated whether to dismiss this ViewController with animation after scan
 * @param invokeAfterDismissal if true, qrCodeScannedListener is invoked after this ViewController was dismissed
 *                             otherwise, it is invoked before dismissal
 */
class QrScannerViewController(
    private val dismissAnimated: Boolean = true,
    private val invokeAfterDismissal: Boolean = true,
    private val qrCodeScannedListener: (String) -> Unit
) : UIViewController() {
    private lateinit var captureSession: AVCaptureSession
    private var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.black()
        captureSession = AVCaptureSession()

        try {
            val videoCaptureDevice = AVCaptureDevice.getDefaultDeviceForMediaType(AVMediaType.Video)
            if (videoCaptureDevice == null) {
                failed()
                return
            }

            val videoInput = AVCaptureDeviceInput(videoCaptureDevice)

            if (captureSession.canAddInput(videoInput)) {
                captureSession.addInput(videoInput)
            } else {
                failed()
                return
            }

            val metaDataOutput = AVCaptureMetadataOutput()
            if (captureSession.canAddOutput(metaDataOutput)) {
                captureSession.addOutput(metaDataOutput)
                metaDataOutput.setMetadataObjectsDelegate(Delegate(), DispatchQueue.getMainQueue())
                metaDataOutput.metadataObjectTypes = mutableListOf(AVMetadataObjectType.QRCode)
            } else {
                failed()
                return
            }

        } catch (t: Throwable) {
            LogUtils.logDebug("QRCode", "Failed to init camera", t)
            failed()

        }

        previewLayer = AVCaptureVideoPreviewLayer(captureSession)
        previewLayer!!.frame = view.layer.bounds
        previewLayer!!.videoGravity = AVLayerVideoGravity.ResizeAspectFill
        view.layer.addSublayer(previewLayer)

        addCloseButton()

        val pasteButton = UIImageView(getIosSystemImage(IMAGE_PASTE, UIImageSymbolScale.Small)).apply {
            isUserInteractionEnabled = true
            tintColor = UIColor.white()
            addGestureRecognizer(UITapGestureRecognizer {
                UIPasteboard.getGeneralPasteboard().string?.let {
                    didScanQrCode(it)
                }
            })
        }
        view.addSubview(pasteButton)
        pasteButton.topToSuperview(topInset = DEFAULT_MARGIN).rightToSuperview(inset = DEFAULT_MARGIN)

        captureSession.startRunning()
    }

    private fun failed() {
        val label = Body1BoldLabel().apply {
            textColor = UIColor.white()
            text = getAppDelegate().texts.get(STRING_ERROR_CAMERA)
            textAlignment = NSTextAlignment.Center
        }

        view.addSubview(label)
        label.centerVertical().widthMatchesSuperview(inset = DEFAULT_MARGIN)
    }

    private fun didScanQrCode(stringValue: String?) {
        if (!invokeAfterDismissal) {
            stringValue?.let { qrCodeScannedListener.invoke(stringValue) }
        }
        dismissViewController(dismissAnimated) {
            if (invokeAfterDismissal) {
                stringValue?.let { qrCodeScannedListener.invoke(stringValue) }
            }
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        if (captureSession.isRunning == false) {
            captureSession.startRunning()
        }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        if (captureSession.isRunning == true) {
            captureSession.stopRunning()
        }
    }

    override fun prefersStatusBarHidden(): Boolean {
        return true
    }

    override fun getSupportedInterfaceOrientations(): UIInterfaceOrientationMask {
        return UIInterfaceOrientationMask.Portrait
    }

    inner class Delegate : AVCaptureMetadataOutputObjectsDelegateAdapter() {
        override fun didOutputMetadataObjects(
            output: AVCaptureOutput?,
            metadataObjects: NSArray<AVMetadataObject>?,
            connection: AVCaptureConnection?
        ) {
            captureSession.stopRunning()

            val stringValue = (metadataObjects?.firstOrNull() as? AVMetadataMachineReadableCodeObject)?.let {
                AudioServices.playSystemSound(AudioServices.SystemSoundVibrate)
                it.stringValue
            }

            didScanQrCode(stringValue)
        }
    }
}