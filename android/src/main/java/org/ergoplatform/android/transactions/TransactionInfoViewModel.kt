package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic

class TransactionInfoViewModel : ViewModel() {
    private var _txInfo = MutableLiveData<TransactionInfo?>()
    val txInfo: LiveData<TransactionInfo?> get() = _txInfo

    val uiLogic = object : TransactionInfoUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun onTransactionInformationFetched(ti: TransactionInfo?) {
            _txInfo.postValue(ti)
        }

    }
}