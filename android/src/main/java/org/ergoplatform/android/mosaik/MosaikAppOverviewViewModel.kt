package org.ergoplatform.android.mosaik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.mosaik.MosaikAppOverviewUiLogic

class MosaikAppOverviewViewModel : ViewModel() {

    val uiLogic = object : MosaikAppOverviewUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope
    }

}