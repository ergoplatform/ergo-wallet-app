package org.ergoplatform.android.mosaik

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.BuildConfig
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.model.MosaikManifest
import java.util.*

class MosaikViewModel : ViewModel() {
    val browserEvent = SingleLiveEvent<String?>()
    val pasteToClipboardEvent = SingleLiveEvent<String?>()
    val showDialogEvent = SingleLiveEvent<MosaikDialog?>()
    val manifestLiveData = MutableLiveData<MosaikManifest?>()

    private var initialized = false

    val getContextFor: (String) -> MosaikContext = { url ->
        MosaikContext(
            MosaikContext.LIBRARY_MOSAIK_VERSION,
            UUID.randomUUID().toString(), // TODO save to DB
            Locale.getDefault().language,
            "Ergo Wallet App (Android)",
            BuildConfig.VERSION_NAME,
            MosaikContext.Platform.PHONE // FIXME tablet/desktop when applicable
        )
    }

    val mosaikRuntime = object : AppMosaikRuntime(getContextFor) {
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

    fun initialize(appUrl: String) {
        if (!initialized) {
            initialized = true
            mosaikRuntime.loadUrlEnteredByUser(appUrl)
        }
    }
}