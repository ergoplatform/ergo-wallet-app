package org.ergoplatform.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.android.R
import org.ergoplatform.android.TokenAmount
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

fun forceShowSoftKeyboard(context: Context) {
    ContextCompat.getSystemService(
        context,
        InputMethodManager::class.java
    )?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

fun hideForcedSoftKeyboard(context: Context, editText: EditText) {
    ContextCompat.getSystemService(
        context,
        InputMethodManager::class.java
    )?.hideSoftInputFromWindow(editText.getWindowToken(), 0)
}

fun TextView.enableLinks() {
    movementMethod = LinkMovementMethod.getInstance()
}

fun inputTextToDouble(amountStr: String?): Double {
    try {
        return if (amountStr.isNullOrEmpty()) 0.0 else amountStr.toDouble()
    } catch (t: Throwable) {
        return 0.0
    }
}

/**
 * fiat is formatted according to users locale, because it is his local currency
 */
fun formatFiatToString(amount: Double, currency: String, context: Context): String {
    return DecimalFormat(context.getString(R.string.format_fiat)).format(amount) +
            " " + currency.toUpperCase(Locale.getDefault())
}

/**
 * Formats token (asset) amounts, always formatted US-style.
 * For larger amounts, 1,120.00 becomes 1.1K, useful for displaying with less space
 */
fun formatTokenAmounts(
    amount: Long,
    decimals: Int,
): String {
    val tokenAmount = TokenAmount(amount, decimals)
    val doubleValue: Double = tokenAmount.toDouble()
    val preciseString = tokenAmount.toString()
    return if (doubleValue < 1000 && preciseString.length < 8 || doubleValue < 1) {
        preciseString
    } else {
        formatDoubleWithPrettyReduction(doubleValue)
    }
}

fun formatDoubleWithPrettyReduction(amount: Double): String {
    val suffixChars = "KMGTPE"
    val formatter = DecimalFormat("###.#", DecimalFormatSymbols(Locale.US))
    formatter.roundingMode = RoundingMode.DOWN

    return if (amount < 999.9) formatter.format(amount)
    else {
        val exp = (ln(amount) / ln(1000.0)).toInt()
        formatter.format(amount / 1000.0.pow(exp.toDouble())) + suffixChars[exp - 1]
    }
}

/**
 * Copies address to system clipboard and shows a Snackbar on given view
 */
fun copyAddressToClipboard(address: String, ctx: Context, view: View?) {
    val clipboard = ContextCompat.getSystemService(ctx, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("", address)
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
