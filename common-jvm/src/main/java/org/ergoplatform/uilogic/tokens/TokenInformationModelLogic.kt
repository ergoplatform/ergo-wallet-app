package org.ergoplatform.uilogic.tokens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.tokens.getHttpContentLink
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.ProgressListener
import org.ergoplatform.utils.fetchHttpGetWithListener
import java.security.MessageDigest

abstract class TokenInformationModelLogic {
    abstract val coroutineScope: CoroutineScope

    var initialized = false
        private set
    var eip4Token: Eip4Token? = null
        private set
    var tokenInformation: TokenInformation? = null
        private set
    var downloadState: StateDownload = StateDownload.NOT_AVAILABLE
        private set
    var downloadPercent: Float = 0f
        private set
    var downloadedData: ByteArray? = null
        private set
    var sha256Check: Boolean? = null
        private set

    fun init(
        tokenId: String,
        database: IAppDatabase,
        preferencesProvider: PreferencesProvider
    ) {
        // Make sure this does not get called twice
        if (initialized)
            return

        initialized = true

        coroutineScope.launch(Dispatchers.IO) {
            TokenInfoManager.getInstance().getTokenInformationFlow(
                tokenId,
                database.tokenDbProvider,
                ErgoApiService.getOrInit(preferencesProvider)
            ).collect {
                tokenInformation = it
                eip4Token = try {
                    it?.toEip4Token()
                } catch (t: Throwable) {
                    null
                }

                val canPreviewContent = canPreviewContent(preferencesProvider)
                downloadState =
                    if (canPreviewContent) StateDownload.NOT_STARTED else StateDownload.NOT_AVAILABLE
                if (canPreviewContent && preferencesProvider.downloadNftContent) {
                    downloadContent(preferencesProvider)
                }

                onTokenInfoUpdated(it)
            }
        }
    }

    private val supportedFormats = listOf("png", "gif", "jpg")

    private fun canPreviewContent(preferencesProvider: PreferencesProvider): Boolean {
        return eip4Token?.let { eip4Token ->
            eip4Token.assetType == Eip4Token.AssetType.NFT_PICTURE
                    && eip4Token.getHttpContentLink(preferencesProvider) != null
        } ?: false
    }

    fun downloadContent(preferencesProvider: PreferencesProvider): Boolean {
        if (downloadState != StateDownload.NOT_STARTED)
            return false

        if (!canPreviewContent(preferencesProvider))
            return false

        preferencesProvider.downloadNftContent = true

        coroutineScope.launch(Dispatchers.IO) {
            downloadState = StateDownload.RUNNING
            onDownloadStateUpdated()
            try {
                val responseBytes = fetchHttpGetWithListener(
                    eip4Token!!.getHttpContentLink(preferencesProvider)!!,
                    object : ProgressListener {
                        override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                            downloadState = if (done) StateDownload.DONE else StateDownload.RUNNING
                            downloadPercent =
                                if (contentLength > 0) bytesRead.toFloat() / contentLength.toFloat() else 0f
                            onDownloadStateUpdated()
                        }
                    })
                downloadedData = responseBytes
                downloadState = StateDownload.DONE

                val contentSha256 = MessageDigest.getInstance("SHA-256").digest(responseBytes)
                sha256Check = eip4Token?.nftContentHash?.let {
                    try {
                        contentSha256.contentEquals(it)
                    } catch (t: Throwable) {
                        null
                    }
                }

                onDownloadStateUpdated()
            } catch (t: Throwable) {
                LogUtils.logDebug("NFT-Content", "Error downloading NFT-Content", t)
                downloadState = StateDownload.ERROR
                onDownloadStateUpdated()
            }
        }
        return true
    }

    abstract fun onTokenInfoUpdated(tokenInformation: TokenInformation?)

    abstract fun onDownloadStateUpdated()

    enum class StateDownload { NOT_AVAILABLE, NOT_STARTED, RUNNING, DONE, ERROR }
}