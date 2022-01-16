package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic

class ErgoPaySigningViewModel: SubmitTransactionViewModel() {
    override val uiLogic = AndroidErgoPaySigningUiLogic()

    private var _uiStateRefresh = MutableLiveData<ErgoPaySigningUiLogic.State?>()
    val uiStateRefresh: LiveData<ErgoPaySigningUiLogic.State?> get() = _uiStateRefresh

    inner class AndroidErgoPaySigningUiLogic: ErgoPaySigningUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyWalletStateLoaded() {
            // TODO Ergo Pay show in UI
        }

        override fun notifyDerivedAddressChanged() {
            // TODO Ergo Pay show in UI
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