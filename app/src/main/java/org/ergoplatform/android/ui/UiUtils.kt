package org.ergoplatform.android.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat

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