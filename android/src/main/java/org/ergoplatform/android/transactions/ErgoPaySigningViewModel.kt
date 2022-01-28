package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic

class ErgoPaySigningViewModel: SubmitTransactionViewModel() {
    override val uiLogic = AndroidErgoPaySigningUiLogic()

    private var _uiStateRefresh = MutableLiveData<ErgoPaySigningUiLogic.State?>()
    val uiStateRefresh: LiveData<ErgoPaySigningUiLogic.State?> get() = _uiStateRefresh

    private var _addressChosen = MutableLiveData<WalletAddress?>()
    val addressChosen: LiveData<WalletAddress?> get() = _addressChosen

    inner class AndroidErgoPaySigningUiLogic: ErgoPaySigningUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyWalletStateLoaded() {
            // not needed, notifyDerivedAddressChanged is always called right after this one
        }

        override fun notifyDerivedAddressChanged() {
            _addressChosen.postValue(uiLogic.derivedAddress)
        }

        override fun notifyUiLocked(locked: Boolean) {
            _lockInterface.postValue(locked)
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            _txWorkDoneLiveData.postValue(txResult)
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            _signingPromptData.postValue(signingPrompt)
        }

        override fun notifyStateChanged(newState: State) {
            _uiStateRefresh.postValue(newState)
        }


    }
}