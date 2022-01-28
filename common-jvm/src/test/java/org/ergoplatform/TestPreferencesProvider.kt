package org.ergoplatform

import org.ergoplatform.persistance.PreferencesProvider

class TestPreferencesProvider : PreferencesProvider() {
    override fun getString(key: String, default: String): String {
        return default
    }

    override fun saveString(key: String, value: String) {

    }

    override fun getLong(key: String, default: Long): Long {
        return default
    }

    override fun saveLong(key: String, value: Long) {

    }

    override fun getFloat(key: String, default: Float): Float {
        return default
    }

    override fun saveFloat(key: String, value: Float) {

    }
}