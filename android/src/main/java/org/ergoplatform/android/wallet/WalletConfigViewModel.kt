package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.wallet.WalletConfigUiLogic

class WalletConfigViewModel : ViewModel() {
    private val _snackbarEvent = SingleLiveEvent<Int>()
    val snackbarEvent: LiveData<Int> = _snackbarEvent
    private val _walletConfig = MutableLiveData<WalletConfig?>()
    val walletConfig: LiveData<WalletConfig?> = _walletConfig

    // we have different use cases for mnemonic decryption, but callbacks don't reflect that
    // in order to have start the right operation after mnemonic is present, we need to save
    // what to do in the view model so it is present after a configuration change as well
    var mnemonicNeededFor: MnemonicNeededFor? = null

    val uiLogic = AndroidWalletConfigUiLogic()

    fun init(walletId: Int, context: Context) {
        viewModelScope.launch {
            uiLogic.initForWallet(walletId, RoomWalletDbProvider(AppDatabase.getInstance(context)))
        }
    }

    fun saveChanges(context: Context, newWalletName: String?) {
        viewModelScope.launch {
            val success = uiLogic.saveChanges(RoomWalletDbProvider(AppDatabase.getInstance(context)), newWalletName)
            if (success) {
                _snackbarEvent.postValue(R.string.label_changes_saved)
            }
        }
    }

    fun deleteWallet(context: Context, walletId: Int) {
        // GlobalScope to let deletion process when fragment is already dismissed
        GlobalScope.launch {
            val database = AppDatabase.getInstance(context)
            val walletDao = database.walletDao()
            val walletConfig = uiLogic.wallet
            walletConfig?.let {
                database.withTransaction {
                    walletConfig.firstAddress?.let { firstAddress ->
                        RoomWalletDbProvider(database).deleteWalletConfigAndStates(firstAddress, walletId)
                    }
                }

                // After we deleted a wallet, we can prune the keystore if it is not needed
                if (walletDao.getAllWalletConfigsSyncronous().filter { it.encryptionType == ENC_TYPE_DEVICE }
                        .isEmpty()) {
                    AndroidEncryptionManager.emptyKeystore()
                }
            }
        }
    }

    fun decryptMnemonicWithPass(password: String): String? {
        uiLogic.wallet?.secretStorage?.let {
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                return deserializeSecrets(String(decryptData!!))
            } catch (t: Throwable) {
                // Password wrong
                return null
            }

        }
        return null
    }

    fun decryptMnemonicWithUserAuth(): String? {
        uiLogic.wallet?.secretStorage?.let {
            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            return deserializeSecrets(String(decryptData!!))
        }
        return null
    }

    inner class AndroidWalletConfigUiLogic: WalletConfigUiLogic() {
        override fun onConfigChanged(value: WalletConfig?) {
            _walletConfig.postValue(value)
        }

    }

    enum class MnemonicNeededFor { DISPLAY_MNEMONIC, SHOW_XPUB }
}