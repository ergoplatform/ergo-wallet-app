package org.ergoplatform.android

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.mosaik.MosaikNotificationSyncManager
import org.ergoplatform.utils.LogUtils
import java.util.concurrent.TimeUnit

class BackgroundSync(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                LogUtils.logDebug(this.javaClass.simpleName, "Background sync starting...")
                val context = applicationContext
                val result = WalletStateSyncManager.getInstance().refreshWalletStates(
                    Preferences(context),
                    AppDatabase.getInstance(context).walletDbProvider
                )

                if (result == WalletStateSyncManager.RefreshResult.DidSyncHasChange) {
                    showAppNotification(
                        context,
                        NOTIF_CHANNEL_ID_BALANCE,
                        NOTIF_ID_BALANCE,
                        context.getString(R.string.desc_balance_notification)
                    )
                }

                val newUnread = MosaikNotificationSyncManager.updateMosaikNotifications(
                    AppDatabase.getInstance(context),
                    AndroidStringProvider(context),
                    onlyCheckForNew = true
                )

                if (newUnread)
                    showAppNotification(
                        context,
                        NOTIF_CHANNEL_ID_DAPPS,
                        NOTIF_ID_DAPP,
                        context.getString(R.string.desc_mosaik_app_notification)
                    )

                Result.success()
            } catch (t: Throwable) {
                LogUtils.logDebug(this.javaClass.simpleName, "Background sync failure", t)
                Result.retry()
            }
        }
    }

    private fun showAppNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        text: String,
    ) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
            context, channelId,
        ).setSmallIcon(R.drawable.ic_ergologo)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(
            notificationId, mBuilder.build()
        )
    }

    companion object {
        const val NOTIF_CHANNEL_ID_BALANCE = "syncNotif"
        const val NOTIF_CHANNEL_ID_DAPPS = "mosaikAppNotif"
        const val NOTIF_ID_BALANCE = 4711
        const val NOTIF_ID_DAPP = 4712
        private const val JOB_TAG = "sync"

        fun rescheduleJob(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val timeIntervalHours = Preferences(context).balanceSyncInterval
            workManager.cancelAllWorkByTag(JOB_TAG)

            if (timeIntervalHours > 0) {
                val syncRequest =
                    PeriodicWorkRequestBuilder<BackgroundSync>(
                        timeIntervalHours, TimeUnit.HOURS
                    ).addTag(JOB_TAG).setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    ).setInitialDelay(timeIntervalHours, TimeUnit.HOURS)
                        .build()

                workManager.enqueue(syncRequest)
            }
        }
    }
}