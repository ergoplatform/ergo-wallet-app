package org.ergoplatform.android

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ergoNetworkType = StageConstants.NETWORK_TYPE
        AppCompatDelegate.setDefaultNightMode(getDayNightMode(applicationContext))
        NodeConnector.getInstance().loadPreferenceValues(applicationContext)
    }
}