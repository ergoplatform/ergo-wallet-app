package org.ergoplatform.mosaik

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getHostname
import java.util.*
import kotlin.math.max

object MosaikNotificationSyncManager {
    val refreshingNotifications = MutableStateFlow(false)

    @OptIn(DelicateCoroutinesApi::class)
    fun startUpdateMosaikNotifications(
        database: IAppDatabase,
        texts: StringProvider,
    ) {
        if (refreshingNotifications.value) return

        refreshingNotifications.value = true

        GlobalScope.launch {
            updateMosaikNotifications(database, texts)
            refreshingNotifications.value = false
        }
    }

    suspend fun updateMosaikNotifications(
        database: IAppDatabase,
        texts: StringProvider,
        onlyCheckForNew: Boolean = false,
    ): Boolean {
        var hasNewUnread = false

        val appsToCheck =
            database.mosaikDbProvider.getAllAppFavoritesByLastVisited().firstOrNull()?.filter {
                it.notificationUrl != null && it.nextNotificationCheck < System.currentTimeMillis()
                        && (!onlyCheckForNew || !it.notificationUnread)
            }
        LogUtils.logDebug(
            "MosaikNotifications",
            "${appsToCheck?.size ?: 0} Mosaik App notifications to check"
        )

        if (!appsToCheck.isNullOrEmpty()) {
            val guidManager = MosaikGuidManager().apply {
                this.appDatabase = database
            }

            val connector = OkHttpBackendConnector(
                OkHttpSingleton.getInstance().newBuilder(),
                getContextFor = { url ->
                    MosaikContext(
                        MosaikContext.LIBRARY_MOSAIK_VERSION,
                        guidManager.getGuidForHost(getHostname(url)),
                        texts.locale.language,
                        "Ergo Wallet App", // FIXME should report correct value
                        "BG Job", // FIXME should report correct value
                        MosaikContext.Platform.PHONE, // FIXME should report correct value
                        (TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000),
                    )
                })

            coroutineScope {
                appsToCheck.forEach { appEntry ->
                    launch(Dispatchers.IO) {
                        try {
                            val newUnread =
                                updateNotificationForMosaikApp(connector, appEntry, database)
                            if (newUnread)
                                hasNewUnread = true
                        } catch (t: Throwable) {
                            LogUtils.logDebug(
                                "MosaikNotifications",
                                "Error checking notifications ${appEntry.notificationUrl}", t
                            )
                        }
                    }
                }
                // wait for all started jobs
                coroutineContext[Job]?.children?.forEach { it.join() }
            }
        }
        return hasNewUnread
    }

    private suspend fun updateNotificationForMosaikApp(
        connector: OkHttpBackendConnector,
        appEntry: MosaikAppEntry,
        appDatabase: IAppDatabase
    ): Boolean {
        val url = connector.getAbsoluteUrl(appEntry.url, appEntry.notificationUrl!!)
        LogUtils.logDebug("MosaikNotifications", "Checking for notifactions at $url")
        val response = connector.checkForNotification(url)
        return appDatabase.mosaikDbProvider.loadAppEntry(appEntry.url)
            ?.let { freshLoadedEntry ->
                val unread = response.message != null &&
                        (freshLoadedEntry.notificationUnread || freshLoadedEntry.lastNotificationMs < response.messageTs)

                appDatabase.mosaikDbProvider.insertOrUpdateAppEntry(
                    freshLoadedEntry.copy(
                        lastNotificationMessage = response.message,
                        nextNotificationCheck = System.currentTimeMillis()
                                + max(response.nextCheck, MIN_CHECK_INTERVAL_MINUTES) * 1000L * 60,
                        lastNotificationMs = response.messageTs,
                        notificationUnread = unread
                    )
                )

                unread && !freshLoadedEntry.notificationUnread
            } ?: false
    }

    const val MIN_CHECK_INTERVAL_MINUTES = 15
}

fun List<MosaikAppEntry>.getUnreadNotificationCount() =
    filter { it.favorite && it.notificationUnread }.size