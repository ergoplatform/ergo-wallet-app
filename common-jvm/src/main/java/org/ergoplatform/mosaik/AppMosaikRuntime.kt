package org.ergoplatform.mosaik

import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.mosaik.model.MosaikContext

abstract class AppMosaikRuntime(getContextFor: (String) -> MosaikContext) :
    MosaikRuntime(
        OkHttpBackendConnector(
            OkHttpSingleton.getInstance().newBuilder(),
            getContextFor
        )
    ) {
    // TODO val dialogHandler = MosaikComposeDialogHandler()

}