package org.ergoplatform.ios.ui

import org.ergoplatform.uilogic.STRING_ERROR_CAMERA
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.audiotoolbox.AudioServices
import org.robovm.apple.avfoundation.*
import org.robovm.apple.dispatch.DispatchQueue
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSTextAlignment
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIInterfaceOrientationMask
import org.robovm.apple.uikit.UIViewController

class QrScannerViewController(val qrCodeScannedListener: (String) -> Unit) : UIViewController() {
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

        captureSession.startRunning()
    }

    fun failed() {
        val label = Body1BoldLabel().apply {
            textColor = UIColor.white()
            text = getAppDelegate().texts.get(STRING_ERROR_CAMERA)
            textAlignment = NSTextAlignment.Center
        }

        view.addSubview(label)
        label.centerVertical().widthMatchesSuperview(inset = DEFAULT_MARGIN)
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

            (metadataObjects?.firstOrNull() as? AVMetadataMachineReadableCodeObject)?.let {
                val stringValue = it.stringValue
                qrCodeScannedListener.invoke(stringValue)
                AudioServices.playSystemSound(AudioServices.SystemSoundVibrate)
            }

            this@QrScannerViewController.dismissViewController(true) {}
        }
    }
}