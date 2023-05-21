package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic
import org.ergoplatform.wallet.addresses.findWalletConfigAndAddressIdx

class TransactionInfoViewModel : ViewModel() {
    private var _txInfo = MutableLiveData<TransactionInfo?>()
    val txInfo: LiveData<TransactionInfo?> get() = _txInfo
    private val _canCancel = MutableLiveData<Boolean>()
    val canCancel: LiveData<Boolean> get() = _canCancel
    val cancelTxPromptSigning = SingleLiveEvent<PromptSigningResult>()
    var walletConfigAndDerivedIdx: Pair<Int, Int>? = null
        private set

    fun doCancelTx(
        walletProvider: WalletDbProvider,
        prefs: PreferencesProvider,
        texts: StringProvider,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            walletConfigAndDerivedIdx = findWalletConfigAndAddressIdx(
                uiLogic.address!!, walletProvider
            )
            cancelTxPromptSigning.postValue(uiLogic.buildCancelTransaction(prefs, texts))
        }
    }

    val uiLogic = object : TransactionInfoUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun onTransactionInformationFetched(ti: TransactionInfo?) {
            _txInfo.postValue(ti)
            _canCancel.postValue(shouldOfferCancelButton())
        }
    }
}