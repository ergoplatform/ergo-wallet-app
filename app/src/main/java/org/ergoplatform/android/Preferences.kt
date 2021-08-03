package org.ergoplatform.android

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

const val NAME_SHAREDPREFS = "ergowallet"
const val KEY_FIAT_CURRENCY = "fiatCurrency"
const val KEY_NODE_URL = "nodeUrl"
const val KEY_EXPLORER_API_URL = "explorerApiUrl"

private fun getSharedPrefs(context: Context) =
    context.getSharedPreferences(NAME_SHAREDPREFS, Context.MODE_PRIVATE)

fun getPrefDisplayCurrency(context: Context): String {
    return getSharedPrefs(context).getString(KEY_FIAT_CURRENCY, "usd") ?: ""
}

fun saveDisplayCurrency(context: Context, currency: String) {
    getSharedPrefs(context).edit().putString(KEY_FIAT_CURRENCY, currency).apply()
}

fun getPrefNodeUrl(context: Context): String {
    return getSharedPrefs(context).getString(KEY_NODE_URL, StageConstants.NODE_API_ADDRESS)!!
}

fun saveNodeUrl(context: Context, nodeUrl: String) {
    var savedNodeUrl = nodeUrl
    if (savedNodeUrl.isEmpty()) {
        savedNodeUrl = StageConstants.NODE_API_ADDRESS
    } else if (!savedNodeUrl.endsWith("/")) {
        savedNodeUrl += "/"
    }

    getSharedPrefs(context).edit().putString(KEY_NODE_URL, savedNodeUrl).apply()
}

fun getPrefExplorerApiUrl(context: Context): String {
    return getSharedPrefs(context).getString(KEY_EXPLORER_API_URL, StageConstants.EXPLORER_API_ADDRESS)!!
}

fun saveExplorerApiUrl(context: Context, explorerApiUrl: String) {
    var savedExplorerApiUrl = explorerApiUrl
    if (savedExplorerApiUrl.isEmpty()) {
        savedExplorerApiUrl = StageConstants.EXPLORER_API_ADDRESS
    } else if (!savedExplorerApiUrl.endsWith("/")) {
        savedExplorerApiUrl += "/"
    }

    getSharedPrefs(context).edit().putString(KEY_EXPLORER_API_URL, savedExplorerApiUrl).apply()
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

fun getLastRefreshMs(context: Context): Long {
    return getSharedPrefs(context).getLong("lastRefreshMs", 0)
}

fun saveLastRefreshMs(context: Context, time: Long) {
    getSharedPrefs(context).edit().putLong("lastRefreshMs", time).apply()
}

fun getLastFiatValue(context: Context): Float {
    return getSharedPrefs(context).getFloat("fiatValue", 0f)
}

fun saveLastFiatValue(context: Context, value: Float) {
    getSharedPrefs(context).edit().putFloat("fiatValue", value).apply()
}
