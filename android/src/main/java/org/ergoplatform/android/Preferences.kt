package org.ergoplatform.android

import StageConstants
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.ergoplatform.persistance.PreferencesProvider

const val NAME_SHAREDPREFS = "ergowallet"
const val KEY_FIAT_CURRENCY = "fiatCurrency"
const val KEY_NODE_URL = "nodeUrl"
const val KEY_EXPLORER_API_URL = "explorerApiUrl"

class Preferences(val context: Context) : PreferencesProvider {
    private fun getSharedPrefs(context: Context) =
        context.getSharedPreferences(NAME_SHAREDPREFS, Context.MODE_PRIVATE)

    override var prefDisplayCurrency: String
        get() = getSharedPrefs(context).getString(KEY_FIAT_CURRENCY, "usd") ?: ""
        set(currency) {
            getSharedPrefs(context).edit().putString(KEY_FIAT_CURRENCY, currency).apply()
        }

    override var prefNodeUrl: String
        get() = getSharedPrefs(context).getString(KEY_NODE_URL, StageConstants.NODE_API_ADDRESS)!!
        set(nodeUrl) {
            var savedNodeUrl = nodeUrl
            if (savedNodeUrl.isEmpty()) {
                savedNodeUrl = StageConstants.NODE_API_ADDRESS
            } else if (!savedNodeUrl.endsWith("/")) {
                savedNodeUrl += "/"
            }

            getSharedPrefs(context).edit().putString(KEY_NODE_URL, savedNodeUrl).apply()
        }

    override var prefExplorerApiUrl: String
        get() = getSharedPrefs(context).getString(
            KEY_EXPLORER_API_URL,
            StageConstants.EXPLORER_API_ADDRESS
        )!!
        set(value) {
            var savedExplorerApiUrl = value
            if (savedExplorerApiUrl.isEmpty()) {
                savedExplorerApiUrl = StageConstants.EXPLORER_API_ADDRESS
            } else if (!savedExplorerApiUrl.endsWith("/")) {
                savedExplorerApiUrl += "/"
            }

            getSharedPrefs(context).edit().putString(KEY_EXPLORER_API_URL, savedExplorerApiUrl)
                .apply()
        }

    override var dayNightMode: Int
        get() = getSharedPrefs(context).getInt(
            "dayNightMode",
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        set(mode) {
            getSharedPrefs(context).edit().putInt("dayNightMode", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }

    override var lastRefreshMs: Long
        get() = getSharedPrefs(context).getLong("lastRefreshMs", 0)
        set(time) {
            getSharedPrefs(context).edit().putLong("lastRefreshMs", time).apply()
        }

    override var lastFiatValue: Float
        get() = getSharedPrefs(context).getFloat("fiatValue", 0f)
        set(value) {
            getSharedPrefs(context).edit().putFloat("fiatValue", value).apply()
        }
}