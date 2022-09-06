package org.ergoplatform

import org.ergoplatform.persistance.PreferencesProvider

class TestPreferencesProvider : PreferencesProvider() {
    private val prefs = HashMap<String, Any?>()

    override fun getString(key: String, default: String): String {
        return (prefs[key] as? String) ?: default
    }

    override fun saveString(key: String, value: String) {
        prefs[key] = value
    }

    override fun getLong(key: String, default: Long): Long {
        return (prefs[key] as? Long) ?: default
    }

    override fun saveLong(key: String, value: Long) {
        prefs[key] = value
    }

    override fun getFloat(key: String, default: Float): Float {
        return (prefs[key] as? Float) ?: default
    }

    override fun saveFloat(key: String, value: Float) {
        prefs[key] = value
    }
}