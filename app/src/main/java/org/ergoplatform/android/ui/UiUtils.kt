package org.ergoplatform.android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

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
    )?.hideSoftInputFromWindow(editText.getWindowToken(), 0);
}

fun inputTextToFloat(amountStr: String?): Float {
    try {
        return if (amountStr == null || amountStr.isEmpty()) 0f else amountStr.toFloat()
    } catch (t: Throwable) {
        return 0f
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
        Log.e("Navigation error", "Could not find action to navigate to action from " + currentDestination?.toString())
    }
}

fun NavController.navigateSafe(directions: NavDirections) {
    navigateSafe(directions.actionId, directions.arguments)
}
