package org.ergoplatform.android.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.uilogic.wallet.SaveWalletUiLogic

class SaveWalletViewModel : ViewModel() {
    var uiLogic: SaveWalletUiLogic? = null

    private var _publicAddress = MutableLiveData<String?>()
    val publicAddress: LiveData<String?> = _publicAddress

    fun init(mnemonic: SecretString, fromRestore: Boolean) {
        if (uiLogic == null) {
            // firing up appkit for the first time needs some time on medium end devices, so do this on
            // background thread while showing infinite progress bar
            viewModelScope.launch(Dispatchers.IO) {
                uiLogic = SaveWalletUiLogic(mnemonic, fromRestore)
                _publicAddress.postValue(uiLogic!!.publicAddress)
            }
        }
    }

    fun switchAddress() {
        uiLogic?.let {
            it.switchAddress()
            _publicAddress.postValue(it.publicAddress)
        }
    }
}