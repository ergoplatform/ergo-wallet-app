package org.ergoplatform.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.ergoplatform.android.R
import org.ergoplatform.transactions.MessageSeverity

fun forceShowSoftKeyboard(context: Context) {
    ContextCompat.getSystemService(
        context,
        InputMethodManager::class.java
    )?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

fun hideForcedSoftKeyboard(context: Context, view: View) {
    ContextCompat.getSystemService(
        context,
        InputMethodManager::class.java
    )?.hideSoftInputFromWindow(view.windowToken, 0)
}

fun TextView.enableLinks() {
    movementMethod = LinkMovementMethod.getInstance()
}

fun openUrlWithBrowser(context: Context, url: String): Boolean {
    return try {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        context.startActivity(browserIntent)
        true
    } catch (t: Throwable) {
        false
    }
}

/**
 * Copies address to system clipboard and shows a Snackbar on given view
 */
fun copyStringToClipboard(string: String, ctx: Context, view: View?) {
    val clipboard = ContextCompat.getSystemService(ctx, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("", string)
    clipboard?.setPrimaryClip(clip)

    view?.let {
        Snackbar.make(it, R.string.label_copied, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.nav_view).show()
    }
}

/**
 * prevents crashes when a button is tapped twice, see
 * https://stackoverflow.com/questions/51060762/illegalargumentexception-navigation-destination-xxx-is-unknown-to-this-navcontr
 */
fun NavController.navigateSafe(
    @IdRes resId: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null,
    navExtras: Navigator.Extras? = null
) {
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)
    if (action != null && currentDestination?.id != action.destinationId) {
        navigate(resId, args, navOptions, navExtras)
    } else {
        Log.e(
            "Navigation error",
            "Could not find action to navigate to action from " + currentDestination?.toString()
        )
    }
}

fun NavController.navigateSafe(directions: NavDirections, navOptions: NavOptions? = null) {
    navigateSafe(directions.actionId, directions.arguments, navOptions)
}

fun setQrCodeToImageView(imageViewQrCode: ImageView, text: String, width: Int, height: Int) {
    val bitmap = convertQrCodeToBitmap(text, width, height)
    imageViewQrCode.setImageBitmap(bitmap)
}

fun convertQrCodeToBitmap(text: String, width: Int, height: Int): Bitmap? {
    return try {
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.encodeBitmap(
            text,
            BarcodeFormat.QR_CODE,
            width,
            height
        )
    } catch (e: Exception) {
        null
    }
}

fun showDialogWithCopyOption(context: Context, message: String) {
    MaterialAlertDialogBuilder(context)
        .setMessage(message)
        .setPositiveButton(R.string.button_copy) { _, _ ->
            val clipboard = ContextCompat.getSystemService(
                context,
                ClipboardManager::class.java
            )
            val clip = ClipData.newPlainText("", message)
            clipboard?.setPrimaryClip(clip)
        }
        .setNegativeButton(R.string.label_dismiss, null)
        .show()
}

fun showSensitiveDataCopyDialog(context: Context, dataToCopy: String) {
    MaterialAlertDialogBuilder(context)
        .setMessage(R.string.desc_copy_sensitive_data)
        .setPositiveButton(R.string.button_copy_sensitive_data) { _, _ ->
            val clipboard = ContextCompat.getSystemService(
                context,
                ClipboardManager::class.java
            )
            val clip = ClipData.newPlainText("", dataToCopy)
            clipboard?.setPrimaryClip(clip)
        }
        .setNegativeButton(R.string.label_cancel, null)
        .show()
}

/**
 * expands on first show. Call this in onCreateView()
 */
fun BottomSheetDialogFragment.expandBottomSheetOnShow() {
    dialog?.setOnShowListener { dialog ->
        val d = dialog as BottomSheetDialog
        d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).state =
                BottomSheetBehavior.STATE_EXPANDED
        }
    }
}

fun postDelayed(delayMs: Long, r: Runnable) {
    Handler(Looper.getMainLooper()).postDelayed(r, delayMs)
}

fun TextView.setTextAndVisibility(newText: String?) {
    newText?.let {
        visibility = View.VISIBLE
        text = it
    } ?: run { visibility = View.GONE }
}

fun Fragment.shareText(textToShare: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, textToShare)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

fun MessageSeverity.getSeverityDrawableResId() =
    when (this) {
        MessageSeverity.NONE -> 0
        MessageSeverity.INFORMATION -> R.drawable.ic_info_100
        MessageSeverity.WARNING -> R.drawable.ic_warning_amber_100
        MessageSeverity.ERROR -> R.drawable.ic_error_outline_100
    }

fun decodeSampledBitmapFromByteArray(
    bitmapBytes: ByteArray,
    minPixelWidth: Int,
    minPixelHeight: Int
): Bitmap {
    // see https://developer.android.com/topic/performance/graphics/load-bitmap.html#read-bitmap

    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size, this)

        // Crashes when displaying images that are too big - calculate inSampleSize

        inSampleSize = calculateInSampleSize(
            this,
            minPixelWidth,
            minPixelHeight
        )

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size, this)
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
