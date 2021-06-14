package org.ergoplatform.android

import android.content.Context

const val NAME_SHAREDPREFS = "ergowallet"

private fun getSharedPrefs(context: Context) =
    context.getSharedPreferences(NAME_SHAREDPREFS, Context.MODE_PRIVATE)

fun getPrefDisplayCurrency(context: Context): String {
    return getSharedPrefs(context).getString("fiatCurrency", "usd") ?: ""
}

fun saveDisplayCurrency(context: Context, currency: String) {
    getSharedPrefs(context).edit().putString("fiatCurrency", currency).apply()
}