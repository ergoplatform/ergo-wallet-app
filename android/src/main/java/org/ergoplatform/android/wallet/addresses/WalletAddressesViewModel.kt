package org.ergoplatform.android.wallet.addresses

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressesUiLogic

class WalletAddressesViewModel : ViewModel() {
    val uiLogic = AndroidWalletAddressesUiLogic()

    var numAddressesToAdd: Int = 1
    val wallet: Wallet? get() = uiLogic.wallet

    private val _addresses = MutableLiveData<List<WalletAddress>>()
    val addresses: LiveData<List<WalletAddress>> = _addresses

    private val _lockProgress = MutableLiveData<Boolean>()
    val lockProgress: LiveData<Boolean> = _lockProgress

    fun init(ctx: Context, walletId: Int) {
        uiLogic.init(RoomWalletDbProvider(AppDatabase.getInstance(ctx)), walletId)
    }


    fun addNextAddresses(ctx: Context, mnemonic: String?) {
        uiLogic.addNextAddresses(
            RoomWalletDbProvider(AppDatabase.getInstance(ctx)),
            Preferences(ctx), numAddressesToAdd, mnemonic
        )
    }

    fun addAddressWithBiometricAuth(ctx: Context) {
        wallet?.walletConfig?.secretStorage?.let {
            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            deserializeSecrets(String(decryptData!!))?.let { mnemonic ->
                addNextAddresses(ctx, mnemonic)
            }
        }
    }

    fun addAddressWithPass(ctx: Context, password: String): Boolean {
        wallet?.walletConfig?.secretStorage?.let {
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                deserializeSecrets(String(decryptData!!))?.let { mnemonic ->
                    addNextAddresses(ctx, mnemonic)
                    return true
                }
            } catch (t: Throwable) {
                // Password wrong
            }

        }
        return false
    }

    inner class AndroidWalletAddressesUiLogic : WalletAddressesUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyNewAddresses() {
            _addresses.postValue(addresses)
        }

        override fun notifyUiLocked(locked: Boolean) {
            _lockProgress.postValue(locked)
        }

    }
}