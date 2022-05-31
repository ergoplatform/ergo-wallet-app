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
import java.util.*

class MosaikViewModel : ViewModel() {
    val browserEvent = SingleLiveEvent<String?>()
    val pasteToClipboardEvent = SingleLiveEvent<String?>()
    val showDialogEvent = SingleLiveEvent<MosaikDialog?>()
    val manifestLiveData = MutableLiveData<MosaikManifest?>()

    private var initialized = false

    val mosaikRuntime = object : AppMosaikRuntime(
        "Ergo Wallet App (Android)",
        BuildConfig.VERSION_NAME,
        { platformType!! },
        MosaikGuidManager(),
    ) {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun openBrowser(url: String): Boolean {
            browserEvent.postValue(url)
            return true
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
    }

    var platformType: MosaikContext.Platform? = null

    fun initialize(appUrl: String, appDb: AppDatabase, platformType: MosaikContext.Platform) {
        this.platformType = platformType
        mosaikRuntime.appDatabase = appDb
        mosaikRuntime.guidManager.appDatabase = appDb
        if (!initialized) {
            initialized = true
            mosaikRuntime.loadUrlEnteredByUser(appUrl)
        }
    }
}