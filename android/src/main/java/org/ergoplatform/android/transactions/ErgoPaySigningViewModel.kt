package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic

class ErgoPaySigningViewModel: ViewModel() {
    val uiLogic = AndroidErgoPaySigningUiLogic()

    private var _uiStateRefresh = MutableLiveData<ErgoPaySigningUiLogic.State?>()
    val uiStateRefresh: LiveData<ErgoPaySigningUiLogic.State?> get() = _uiStateRefresh

    inner class AndroidErgoPaySigningUiLogic: ErgoPaySigningUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyStateChanged(newState: State) {
            _uiStateRefresh.postValue(newState)
        }


    }
}