package org.ergoplatform.android

import StageConstants
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.isErgoMainNet
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

        createNotificationChannel()

        if (preferences.enableAppLock) {
            val context = applicationContext
            val bmm = BiometricManager.from(context)
            if (bmm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS
            )
                shouldLockApp = true
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.title_balance_notification)
            val description = getString(R.string.title_balance_notification)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                BackgroundSync.NOTIF_CHANNEL_ID_SYNC,
                name, importance
            )
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private var lastInteraction = 0L
    var shouldLockApp = false
        private set

    fun isAppLocked(): Boolean =
        shouldLockApp && System.currentTimeMillis() - lastInteraction >= 2L * 60 * 1000L

    fun appUnlocked() {
        lastInteraction = System.currentTimeMillis()
    }

    fun userInteracted() {
        if (!isAppLocked())
            lastInteraction = System.currentTimeMillis()
    }
}