package org.ergoplatform.android

import StageConstants
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.mosaik.MosaikLogger
import org.ergoplatform.utils.LogUtils

class App : Application() {

    companion object {
        var lastStackTrace: String? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isErgoMainNet = (StageConstants.NETWORK_TYPE == NetworkType.MAINNET)
        val preferences = Preferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(preferences.dayNightMode)
        WalletStateSyncManager.getInstance()
            .loadPreferenceValues(preferences, AppDatabase.getInstance(applicationContext))

        LogUtils.stackTraceLogger = { lastStackTrace = it }
        LogUtils.logDebug = BuildConfig.DEBUG
        MosaikLogger.logger = { severity, msg, throwable ->
            LogUtils.logDebug("Mosaik", "$severity: $msg", throwable)
        }
    }
}