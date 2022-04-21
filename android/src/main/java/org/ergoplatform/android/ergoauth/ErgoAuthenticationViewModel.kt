package org.ergoplatform.android.ergoauth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic

class ErgoAuthenticationViewModel: ViewModel() {
    val uiLogic = AndroidUiLogic()

    private val _state = MutableLiveData(ErgoAuthUiLogic.State.FETCHING_DATA)
    val state: LiveData<ErgoAuthUiLogic.State?> get() = _state

    inner class AndroidUiLogic: ErgoAuthUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyStateChanged(newState: State) {
            _state.postValue(newState)
        }
    }
}