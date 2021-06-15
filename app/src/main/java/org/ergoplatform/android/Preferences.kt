package org.ergoplatform.android

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

const val NAME_SHAREDPREFS = "ergowallet"

private fun getSharedPrefs(context: Context) =
    context.getSharedPreferences(NAME_SHAREDPREFS, Context.MODE_PRIVATE)

fun getPrefDisplayCurrency(context: Context): String {
    return getSharedPrefs(context).getString("fiatCurrency", "usd") ?: ""
}

fun saveDisplayCurrency(context: Context, currency: String) {
    getSharedPrefs(context).edit().putString("fiatCurrency", currency).apply()
}

fun getDayNightMode(context: Context): Int {
    return getSharedPrefs(context).getInt(
        "dayNightMode",
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )
}

fun changeDayNightMode(context: Context, @AppCompatDelegate.NightMode mode: Int) {
    getSharedPrefs(context).edit().putInt("dayNightMode", mode).apply()
    AppCompatDelegate.setDefaultNightMode(mode)
}