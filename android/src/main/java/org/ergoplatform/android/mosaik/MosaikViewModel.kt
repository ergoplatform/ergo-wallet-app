package org.ergoplatform.android.mosaik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.BuildConfig
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.AppMosaikRuntime
import java.util.*

class MosaikViewModel : ViewModel() {
    val browserEvent = SingleLiveEvent<String?>()
    val pasteToClipboardEvent = SingleLiveEvent<String?>()
    val showDialogEvent = SingleLiveEvent<MosaikDialog?>()

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

    }

    init {
        mosaikRuntime.loadMosaikApp("http://10.0.2.2:8080")
    }
}