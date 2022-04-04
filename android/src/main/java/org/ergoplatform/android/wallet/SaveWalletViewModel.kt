package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.uilogic.wallet.SaveWalletUiLogic

class SaveWalletViewModel : ViewModel() {
    var uiLogic: SaveWalletUiLogic? = null

    private var _publicAddress = MutableLiveData<String?>()
    val publicAddress: LiveData<String?> = _publicAddress

    private var _derivedAddressNum = MutableLiveData(0)
    val derivedAddressNum: LiveData<Int> get() = _derivedAddressNum

    fun init(mnemonic: SecretString, fromRestore: Boolean, context: Context) {
        if (uiLogic == null) {
            // firing up appkit for the first time needs some time on medium end devices, so do this on
            // background thread while showing infinite progress bar
            viewModelScope.launch(Dispatchers.IO) {
                uiLogic = SaveWalletUiLogic(mnemonic, fromRestore)
                _publicAddress.postValue(uiLogic!!.publicAddress)

                startDerivedAddressesSearch(context)
            }
        }
    }

    fun switchAddress(context: Context) {
        uiLogic?.let { uiLogic ->
            uiLogic.switchAddress()
            _publicAddress.postValue(uiLogic.publicAddress)
            viewModelScope.launch { startDerivedAddressesSearch(context) }
        }
    }

    private suspend fun startDerivedAddressesSearch(context: Context) {
        uiLogic!!.startDerivedAddressesSearch(
            ErgoApiService.getOrInit(Preferences(context)),
            AppDatabase.getInstance(context).walletDbProvider
        ) { _derivedAddressNum.postValue(it) }
    }
}