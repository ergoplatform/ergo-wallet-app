package org.ergoplatform.android

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.ergoplatform.NodeConnector
import org.ergoplatform.appkit.NetworkType

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        isErgoMainNet = (StageConstants.NETWORK_TYPE == NetworkType.MAINNET)
        val preferences = Preferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(preferences.dayNightMode)
        NodeConnector.getInstance().loadPreferenceValues(preferences)
    }
}