package org.ergoplatform.android

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.ergoplatform.persistance.*

const val KEY_DAYNIGHTMODE = "dayNightMode"

class Preferences(val context: Context) : PreferencesProvider() {
    private fun getSharedPrefs(context: Context) =
        context.getSharedPreferences(NAME_SHAREDPREFS, Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String {
        return getSharedPrefs(context).getString(key, default) ?: default
    }

    override fun saveString(key: String, value: String) {
        getSharedPrefs(context).edit().putString(key, value).apply()
    }

    override fun getLong(key: String, default: Long): Long {
        return getSharedPrefs(context).getLong(key, default)
    }

    override fun saveLong(key: String, value: Long) {
        getSharedPrefs(context).edit().putLong(key, value).apply()
    }

    override fun getFloat(key: String, default: Float): Float {
        return getSharedPrefs(context).getFloat(key, default)
    }

    override fun saveFloat(key: String, value: Float) {
        getSharedPrefs(context).edit().putFloat(key, value).apply()
    }

    var dayNightMode: Int
        get() {
            return getSharedPrefs(context).getInt(
                KEY_DAYNIGHTMODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }
        set(mode) {
            getSharedPrefs(context).edit().putInt(KEY_DAYNIGHTMODE, mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }
}