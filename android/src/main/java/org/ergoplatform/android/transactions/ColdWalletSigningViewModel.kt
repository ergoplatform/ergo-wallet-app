package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.transactions.SigningResult
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