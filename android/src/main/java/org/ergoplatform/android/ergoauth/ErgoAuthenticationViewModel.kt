package org.ergoplatform.android.ergoauth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ergoauth.ErgoAuthRequest
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic

class ErgoAuthenticationViewModel: ViewModel() {
    val uiLogic = AndroidUiLogic()

    private val _signingRequest = MutableLiveData<ErgoAuthRequest?>()
    val authRequest: LiveData<ErgoAuthRequest?> get() = _signingRequest

    inner class AndroidUiLogic: ErgoAuthUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyAuthRequestFetched() {
            _signingRequest.postValue(ergAuthRequest)
        }

    }
}