package org.ergoplatform.uilogic.mosaik

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.mosaik.MosaikRuntime
import org.ergoplatform.mosaik.OkHttpBackendConnector
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import java.util.*

class MosaikUiLogic(
    private val appName: String,
    private val appVersion: String,
    val coroutineScope: () -> CoroutineScope,
) {
    // TODO val dialogHandler = MosaikComposeDialogHandler()

    private val mosaikContext = MosaikContext(
        0, // FIXME use correct version
        UUID.randomUUID().toString(), // TODO save to DB
        Locale.getDefault().language,
        appName,
        appVersion,
        MosaikContext.Platform.PHONE // FIXME tablet/desktop when applicable
    )

    private val backendConnector = OkHttpBackendConnector(OkHttpSingleton.getInstance().newBuilder())

    private val _manifestFlow = MutableStateFlow<MosaikManifest?>(null)
    val maniFestFlow: StateFlow<MosaikManifest?> get() = _manifestFlow

    private val runtime =
        MosaikRuntime(
            coroutineScope = coroutineScope,
            backendConnector = backendConnector,
            mosaikContext = mosaikContext,
            showDialog = { dialog ->
                // TODO
            },
            pasteToClipboard = { text ->
                // TODO
            },
            openBrowser = { url ->
                // TODO
                true
            },
            appLoaded = { _manifestFlow.value = it }
        )

    val viewTree get() = runtime.viewTree

    fun loadApp(url: String) {
        runtime.loadMosaikApp(url)
    }
}