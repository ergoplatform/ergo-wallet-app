package org.ergoplatform.mosaik

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.persistance.CacheFileManager
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_BUTTON_CHOOSE_ADDRESS
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getHostname
import org.ergoplatform.utils.normalizeUrl
import org.ergoplatform.wallet.addresses.getAddressLabel
import java.util.*

abstract class AppMosaikRuntime(
    val appName: String,
    val appVersionName: String,
    val platformType: () -> MosaikContext.Platform,
    val guidManager: MosaikGuidManager,
) : MosaikRuntime(
    OkHttpBackendConnector(
        OkHttpSingleton.getInstance().newBuilder(),
        { url ->
            MosaikContext(
                MosaikContext.LIBRARY_MOSAIK_VERSION,
                guidManager.getGuidForHost(getHostname(url)),
                Locale.getDefault().language,
                appName,
                appVersionName,
                platformType()
            )
        }
    )
) {

    lateinit var appDatabase: IAppDatabase
    var cacheFileManager: CacheFileManager? = null

    init {
        appLoaded = { manifest ->
            coroutineScope.launch {
                saveVisitToDb(manifest)
                onAppNavigated(manifest)
            }
        }
    }

    private suspend fun saveVisitToDb(manifest: MosaikManifest) {
        val normalizedUrl = normalizedAppUrl!!

        val formerAppEntry = appDatabase.mosaikDbProvider.loadAppEntry(normalizedUrl)

        val lastVisit = formerAppEntry?.lastVisited ?: 0
        val timeStampNow = System.currentTimeMillis()
        val fileName = formerAppEntry?.iconFile ?: UUID.randomUUID().toString()

        if (manifest.iconUrl != null &&
            (timeStampNow - lastVisit > 1000L * 60 * 30
                    || cacheFileManager?.fileExists(fileName) == false
                    || manifest.appName != formerAppEntry?.name)
        ) {
            cacheFileManager?.let { cacheFileManager ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val image =
                            backendConnector.fetchImage(manifest.iconUrl!!, appUrl!!, appUrl)
                        if (image.size <= 250000)
                            cacheFileManager.saveFileContent(fileName, image)
                    } catch (t: Throwable) {
                        // Image could not be loaded
                        LogUtils.logDebug("AppMosaikRuntime", "Error saving appicon", t)
                    }
                }
            }
        } else if (manifest.iconUrl == null) {
            // delete icon file
            cacheFileManager?.deleteFile(fileName)
        }

        val newAppEntry = MosaikAppEntry(
            normalizedUrl,
            manifest.appName,
            manifest.appDescription,
            iconFile = fileName,
            timeStampNow,
            favorite = formerAppEntry?.favorite ?: false,
        )

        isFavoriteApp = newAppEntry.favorite

        appDatabase.mosaikDbProvider.insertOrUpdateAppEntry(newAppEntry)
        val hostName = getHostname(appUrl!!)
        appDatabase.mosaikDbProvider.insertOrUpdateAppHost(
            MosaikAppHost(
                hostName,
                guidManager.getGuidForHost(hostName)
            )
        )
    }

    var isFavoriteApp = false

    abstract fun onAppNavigated(manifest: MosaikManifest)

    abstract fun noAppLoaded(cause: Throwable)

    fun loadUrlEnteredByUser(appUrl: String) {
        val loadUrl =
            if (!appUrl.contains(' ') && !appUrl.contains("://"))
                "https://$appUrl"
            else
                appUrl

        loadMosaikApp(loadUrl)
    }

    fun switchFavorite() {
        normalizedAppUrl?.let { appUrl ->
            coroutineScope.launch {
                appDatabase.mosaikDbProvider.loadAppEntry(appUrl)?.let { appEntry ->
                    val newEntry = appEntry.copy(favorite = !appEntry.favorite)
                    appDatabase.mosaikDbProvider.insertOrUpdateAppEntry(newEntry)
                    isFavoriteApp = newEntry.favorite
                    onAppNavigated(appManifest!!)
                }

            }
        }
    }

    override fun showError(error: Throwable) {
        // check if this error is on first app load, we show a retry button in this case
        if (viewTree.content == null) {
            noAppLoaded(error)
        } else {
            super.showError(error)
        }
    }

    private val normalizedAppUrl get() = appUrl?.let { normalizeUrl(it) }

    lateinit var stringProvider: StringProvider

    override fun formatString(string: StringConstant, values: String?): String {
        val appConstant = when (string) {
            StringConstant.ChooseAddress -> STRING_BUTTON_CHOOSE_ADDRESS
        }
        return values?.let {
            stringProvider.getString(appConstant, values)
        } ?: stringProvider.getString(appConstant)

    }

    override fun getErgoAddressLabel(ergoAddress: String): String? =
    // TODO Mosaik runblocking here works because db is fast and Compose implementation
        //  remembers - but used addresses should be cached or loaded beforehand
        runBlocking {
            val derivedAddress =
                guidManager.appDatabase.walletDbProvider.loadWalletAddress(ergoAddress)

            val walletConfig = guidManager.appDatabase.walletDbProvider.loadWalletByFirstAddress(
                derivedAddress?.walletFirstAddress ?: ergoAddress
            )

            walletConfig?.let {
                (walletConfig.displayName?.let { "$it - " } ?: "") +
                        (derivedAddress ?: WalletAddress(0, ergoAddress, 0, ergoAddress, null))
                            .getAddressLabel(stringProvider)
            }
        }

    override fun isErgoAddressValid(ergoAddress: String): Boolean =
        isValidErgoAddress(ergoAddress)
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