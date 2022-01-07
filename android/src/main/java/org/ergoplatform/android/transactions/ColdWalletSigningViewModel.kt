package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.transactions.ColdWalletSigningUiLogic

class ColdWalletSigningViewModel : ViewModel() {

    private val _transactionInfo = MutableLiveData<TransactionInfo?>()
    val transactionInfo: LiveData<TransactionInfo?> = _transactionInfo
    private val _lockInterface = MutableLiveData<Boolean>()
    val lockInterface: LiveData<Boolean> = _lockInterface
    private val _signingResult = MutableLiveData<SigningResult?>()
    val signingResult: LiveData<SigningResult?> = _signingResult

    private val uiLogic = AndroidColdWalletSigningUiLogic()
    val wallet get() = uiLogic.wallet
    val signedQrCode get() = uiLogic.signedQrCode
    val pagesQrCode get() = uiLogic.pagesQrCode
    val pagesAdded get() = uiLogic.pagesAdded

    fun addQrCodeChunk(qrCodeChunk: String) {
        val ti = uiLogic.addQrCodeChunk(qrCodeChunk)
        _transactionInfo.postValue(ti)
    }

    fun setWalletId(walletId: Int, ctx: Context) {
        uiLogic.setWalletId(walletId, RoomWalletDbProvider(AppDatabase.getInstance(ctx)))
    }

    fun signTxWithPassword(password: String, texts: StringProvider): Boolean {
        wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: String?
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                mnemonic = deserializeSecrets(String(decryptData!!))
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
            val mnemonic: String?

            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            mnemonic = deserializeSecrets(String(decryptData!!))

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