package org.ergoplatform.mosaik

import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest

abstract class AppMosaikRuntime(getContextFor: (String) -> MosaikContext) :
    MosaikRuntime(
        OkHttpBackendConnector(
            OkHttpSingleton.getInstance().newBuilder(),
            getContextFor
        )
    ) {

    init {
        appLoaded = { onAppNavigated(it) }
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