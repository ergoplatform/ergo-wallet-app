package org.ergoplatform.android.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.ergoplatform.android.R

class QrScannerActivity : CaptureActivity() {
    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.qrscanner_activity)

        findViewById<ImageView>(R.id.paste_button).setOnClickListener {
            pasteFromClipboard()
        }

        return (findViewById<View>(R.id.zxing_barcode_scanner) as DecoratedBarcodeView)
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text?.let { text ->
            val intent = CaptureManager.resultIntent(
                BarcodeResult(
                    Result(text.toString(), null, null, BarcodeFormat.QR_CODE),
                    null
                ), null
            )
            setResult(RESULT_OK, intent)
            finish()
        }

    }

    companion object {
        private val setOfCodes = setOf(IntentIntegrator.QR_CODE)

        fun startFromActivity(activity: Activity) {
            IntentIntegrator(activity)
                .setCaptureActivity(QrScannerActivity::class.java)
                .initiateScan(setOfCodes)
        }

        fun startFromFragment(fragment: Fragment) {
            IntentIntegrator
                .forSupportFragment(fragment)
                .setCaptureActivity(QrScannerActivity::class.java)
                .initiateScan(setOfCodes)
        }
    }
}