package org.ergoplatform.android

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import org.ergoplatform.persistance.*

const val KEY_DAYNIGHTMODE = "dayNightMode"

class Preferences(context: Context) : PreferencesProvider() {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(NAME_SHAREDPREFS, Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String {
        return prefs.getString(key, default) ?: default
    }

    override fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getLong(key: String, default: Long): Long {
        return prefs.getLong(key, default)
    }

    override fun saveLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getFloat(key: String, default: Float): Float {
        return prefs.getFloat(key, default)
    }

    override fun saveFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    var dayNightMode: Int
        get() {
            return prefs.getInt(
                KEY_DAYNIGHTMODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }
        set(mode) {
            prefs.edit().putInt(KEY_DAYNIGHTMODE, mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }
}