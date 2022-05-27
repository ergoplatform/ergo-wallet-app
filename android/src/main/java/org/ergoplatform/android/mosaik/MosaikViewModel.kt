package org.ergoplatform.android.mosaik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.ergoplatform.android.BuildConfig
import org.ergoplatform.uilogic.mosaik.MosaikUiLogic

class MosaikViewModel : ViewModel() {

    val uiLogic = MosaikUiLogic(
        "Ergo Wallet App (Android)",
        BuildConfig.VERSION_NAME,
        coroutineScope = { viewModelScope }
    )

    init {
        uiLogic.loadApp("http://10.0.2.2:8080")
    }
}