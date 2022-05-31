package org.ergoplatform.mosaik

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.utils.getHostname
import org.ergoplatform.utils.normalizeUrl
import java.util.*
import kotlin.collections.HashMap

abstract class AppMosaikRuntime(
    val appName: String,
    val appVersionName: String,
    val platformType: MosaikContext.Platform,
    val guidManager: MosaikGuidManager,
) :
    MosaikRuntime(
        OkHttpBackendConnector(
            OkHttpSingleton.getInstance().newBuilder(),
            { url ->
                MosaikContext(
                    MosaikContext.LIBRARY_MOSAIK_VERSION,
                    guidManager.getGuidForHost(getHostname(url)),
                    Locale.getDefault().language,
                    appName,
                    appVersionName,
                    platformType
                )
            }
        )
    ) {

    lateinit var appDatabase: IAppDatabase

    init {
        appLoaded = { manifest ->
            saveVisitToDb(manifest)
            onAppNavigated(manifest)
        }
    }

    private fun saveVisitToDb(manifest: MosaikManifest) {
        coroutineScope.launch {
            appDatabase.mosaikDbProvider.insertOrUpdateAppEntry(
                MosaikAppEntry(
                    normalizeUrl(appUrl!!),
                    manifest.appName,
                    manifest.appDescription,
                    icon = null, // TODO load and save when loaded, do not overwrite
                    System.currentTimeMillis(),
                    favorite = false, // TODO do not overwrite
                )
            )
            val hostName = getHostname(appUrl!!)
            appDatabase.mosaikDbProvider.insertOrUpdateAppHost(
                MosaikAppHost(
                    hostName,
                    guidManager.getGuidForHost(hostName)
                )
            )
        }
    }

    abstract fun onAppNavigated(manifest: MosaikManifest)

    fun loadUrlEnteredByUser(appUrl: String) {
        val loadUrl =
            if (!appUrl.contains(' ') && !appUrl.contains("://"))
                "https://$appUrl"
            else
                appUrl

        loadMosaikApp(loadUrl)
    }
}

class MosaikGuidManager {
    lateinit var appDatabase: IAppDatabase

    private val guidMap = HashMap<String, String>()

    /**
     * returns guid for a given hostname from cache map, database or newly calculated
     */
    internal fun getGuidForHost(hostname: String): String {
        return if (guidMap.containsKey(hostname)) {
            guidMap[hostname]!!
        } else {
            val hostInfo = runBlocking {
                appDatabase.mosaikDbProvider.getMosaikHostInfo(hostname)
            }

            return if (hostInfo != null) {
                hostInfo.guid
            } else {
                val guid = UUID.randomUUID().toString()
                guidMap[hostname] = guid
                guid
            }
        }
    }
}