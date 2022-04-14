package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.transactions.ColdWalletSigningUiLogic

class ColdWalletSigningViewModel : ViewModel() {

    private val _lockInterface = MutableLiveData<Boolean>()
    val lockInterface: LiveData<Boolean> = _lockInterface
    private val _signingResult = MutableLiveData<SigningResult?>()
    val signingResult: LiveData<SigningResult?> = _signingResult

    val uiLogic = AndroidColdWalletSigningUiLogic()
    val wallet get() = uiLogic.wallet
    val signedQrCode get() = uiLogic.signedQrCode

    fun setWalletId(walletId: Int, ctx: Context) {
        uiLogic.setWalletId(walletId, RoomWalletDbProvider(AppDatabase.getInstance(ctx)))
    }

    fun signTxWithPassword(password: String, texts: StringProvider): Boolean {
        wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: SigningSecrets?
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                mnemonic = SigningSecrets.fromBytes(decryptData!!)
            } catch (t: Throwable) {
                // Password wrong
                return false
            }

            if (mnemonic == null) {
                // deserialization error, corrupted db data
                return false
            }

            uiLogic.signTxWithMnemonicAsync(mnemonic, texts)

            return true
        }

        return false
    }

    fun signTxUserAuth(texts: StringProvider) {
        // we don't handle exceptions here by intention: we throw them back to the caller which
        // will show a snackbar to give the user a hint what went wrong
        wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: SigningSecrets?

            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            mnemonic = SigningSecrets.fromBytes(decryptData!!)

            uiLogic.signTxWithMnemonicAsync(mnemonic!!, texts)

        }
    }

    inner class AndroidColdWalletSigningUiLogic : ColdWalletSigningUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyUiLocked(locked: Boolean) {
            _lockInterface.postValue(locked)
        }

        override fun notifySigningResult(ergoTxResult: SigningResult) {
            _signingResult.postValue(ergoTxResult)
        }

    }
}