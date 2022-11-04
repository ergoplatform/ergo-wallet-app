package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.transactions.SubmitTransactionUiLogic

abstract class SubmitTransactionViewModel : ViewModel() {
    abstract val uiLogic: SubmitTransactionUiLogic

    protected val _lockInterface = MutableLiveData<Boolean>()
    val lockInterface: LiveData<Boolean> = _lockInterface
    protected val _address = MutableLiveData<WalletAddress?>()
    val address: LiveData<WalletAddress?> = _address
    protected val _txWorkDoneLiveData = SingleLiveEvent<TransactionResult>()
    val txWorkDoneLiveData: LiveData<TransactionResult> = _txWorkDoneLiveData
    var signingPromptData: String? = null
        protected set
}