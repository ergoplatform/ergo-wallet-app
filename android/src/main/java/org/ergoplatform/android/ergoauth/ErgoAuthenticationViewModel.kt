package org.ergoplatform.android.ergoauth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic

class ErgoAuthenticationViewModel: ViewModel() {
    val uiLogic = AndroidUiLogic()

    private val _state = MutableLiveData(ErgoAuthUiLogic.State.FETCHING_DATA)
    val state: LiveData<ErgoAuthUiLogic.State?> get() = _state

    fun startAuthenticationFromBiometrics(context: Context) {
        uiLogic.walletConfig?.secretStorage?.let {
            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            val secrets = SigningSecrets.fromBytes(decryptData!!)
            uiLogic.startResponse(secrets!!, AndroidStringProvider(context))
        }
    }

    fun startAuthenticationFromPassword(password: String, texts: StringProvider): Boolean {
        return uiLogic.walletConfig?.secretStorage?.let {
            return try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                val secrets = SigningSecrets.fromBytes(decryptData!!)
                uiLogic.startResponse(secrets!!, texts)
                true
            } catch (t: Throwable) {
                false
            }
        } ?: false
    }

    inner class AndroidUiLogic: ErgoAuthUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyStateChanged(newState: State) {
            _state.postValue(newState)
        }
    }
}