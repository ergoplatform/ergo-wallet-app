package org.ergoplatform.android.mosaik

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.BuildConfig
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.MosaikGuidManager
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.actions.ErgoPayAction
import org.ergoplatform.persistance.CacheFileManager
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.StringProvider

class MosaikViewModel : ViewModel() {
    val browserEvent = SingleLiveEvent<String?>()
    val pasteToClipboardEvent = SingleLiveEvent<String?>()
    val showDialogEvent = SingleLiveEvent<MosaikDialog?>()
    val showAddressChooserEvent = SingleLiveEvent<String?>()
    val manifestLiveData = MutableLiveData<MosaikManifest?>()
    val noAppLiveData = MutableLiveData<Throwable?>()

    private var initialized = false

    var valueIdForAddressChooser: String? = null
    var walletForAddressChooser: Wallet? = null

    val mosaikRuntime = object : AppMosaikRuntime(
        "Ergo Wallet App (Android)",
        BuildConfig.VERSION_NAME,
        { platformType!! },
        MosaikGuidManager(),
    ) {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun openBrowser(url: String) {
            browserEvent.postValue(url)
        }

        override fun pasteToClipboard(text: String) {
            pasteToClipboardEvent.postValue(text)
        }

        override fun showDialog(dialog: MosaikDialog) {
            showDialogEvent.postValue(dialog)
        }

        override fun onAppNavigated(manifest: MosaikManifest) {
            manifestLiveData.postValue(manifest)
        }

        override fun noAppLoaded(cause: Throwable) {
            noAppLiveData.postValue(cause)
        }

        override fun runErgoPayAction(action: ErgoPayAction) {
            TODO("Not yet implemented")
        }

        override fun showErgoAddressChooser(valueId: String) {
            showAddressChooserEvent.postValue(valueId)
        }
    }

    var platformType: MosaikContext.Platform? = null

    fun initialize(
        appUrl: String,
        appDb: AppDatabase,
        platformType: MosaikContext.Platform,
        stringProvider: StringProvider,
        cacheFileManager: CacheFileManager?
    ) {
        this.platformType = platformType
        mosaikRuntime.appDatabase = appDb
        mosaikRuntime.guidManager.appDatabase = appDb
        mosaikRuntime.cacheFileManager = cacheFileManager
        mosaikRuntime.stringProvider = stringProvider
        if (!initialized) {
            initialized = true
            mosaikRuntime.loadUrlEnteredByUser(appUrl)
        }
    }

    fun retryLoading(appUrl: String) {
        noAppLiveData.postValue(null)
        mosaikRuntime.loadUrlEnteredByUser(appUrl)
    }
}