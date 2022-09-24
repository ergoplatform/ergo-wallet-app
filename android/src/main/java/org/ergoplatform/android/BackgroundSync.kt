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
                    showBalanceChangeNotification(context)
                }

                Result.success()
            } catch (t: Throwable) {
                LogUtils.logDebug(this.javaClass.simpleName, "Background sync failure", t)
                Result.retry()
            }
        }
    }

    private fun showBalanceChangeNotification(context: Context) {
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
            context, NOTIF_CHANNEL_ID_SYNC
        ).setSmallIcon(R.drawable.ic_ergologo)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.desc_balance_notification))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(
            NOTIF_ID_SYNC, mBuilder.build()
        )
    }

    companion object {
        const val NOTIF_CHANNEL_ID_SYNC = "syncNotif"
        const val NOTIF_ID_SYNC = 4711
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