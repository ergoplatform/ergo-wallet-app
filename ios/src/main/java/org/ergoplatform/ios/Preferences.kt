package org.ergoplatform.ios

import org.ergoplatform.isErgoMainNet
import org.ergoplatform.persistance.PreferencesProvider
import org.robovm.apple.foundation.NSMutableDictionary
import org.robovm.apple.foundation.NSObject
import org.robovm.apple.foundation.NSString
import java.io.File

const val KEY_APPLOCK = "appLock"

class Preferences : PreferencesProvider() {
    private val iosPreferences: IOSPreferences

    init {
        val name = if (isErgoMainNet) "wallet_prefs" else "wallet_test"

        val libraryPath = File(System.getenv("HOME"), "Library")
        val finalPath = File(libraryPath, "$name.plist")
        var nsDictionary =
            NSMutableDictionary.read(finalPath) as? NSMutableDictionary<NSString?, NSObject?>

        // if it fails to get an existing dictionary, create a new one.
        if (nsDictionary == null) {
            nsDictionary = NSMutableDictionary()
            nsDictionary.write(finalPath, false)
        }
        iosPreferences = IOSPreferences(nsDictionary, finalPath.absolutePath)
    }

    override fun getString(key: String, default: String): String {
        return iosPreferences.getString(key, default)
    }

    override fun saveString(key: String, value: String) {
        iosPreferences.putString(key, value).flush()
    }

    override fun getLong(key: String, default: Long): Long {
        return iosPreferences.getLong(key, default)
    }

    override fun saveLong(key: String, value: Long) {
        iosPreferences.putLong(key, value).flush()
    }

    override fun getFloat(key: String, default: Float): Float {
        return iosPreferences.getFloat(key, default)
    }

    override fun saveFloat(key: String, value: Float) {
        iosPreferences.putFloat(key, value).flush()
    }

    var enableAppLock: Boolean
        get() = getBoolean(KEY_APPLOCK, false)
        set(value) = saveBoolean(KEY_APPLOCK, value)

}