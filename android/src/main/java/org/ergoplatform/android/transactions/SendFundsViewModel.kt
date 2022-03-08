package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ErgoApiService
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic

/**
 * Holding state of the send funds screen (thus to be expected to get complicated)
 */
class SendFundsViewModel : SubmitTransactionViewModel() {
    override val uiLogic = AndroidSendFundsUiLogic()

    private val _walletName = MutableLiveData<String>()
    val walletName: LiveData<String> = _walletName
    private val _walletBalance = MutableLiveData<ErgoAmount>()
    val walletBalance: LiveData<ErgoAmount> = _walletBalance
    private val _grossAmount = MutableLiveData<ErgoAmount>().apply {
        value = ErgoAmount.ZERO
    }
    val grossAmount: LiveData<ErgoAmount> = _grossAmount
    private val _txId = MutableLiveData<String?>()
    val txId: LiveData<String?> = _txId

    // the live data gets data posted on adding or removing tokens, not on every amount change
    private val _tokensChosenLiveData = MutableLiveData<List<String>>()
    val tokensChosenLiveData: LiveData<List<String>> = _tokensChosenLiveData

    private val _errorMessageLiveData = SingleLiveEvent<String>()
    val errorMessageLiveData: LiveData<String> = _errorMessageLiveData

    fun initWallet(ctx: Context, walletId: Int, derivationIdx: Int, paymentRequest: String?) {
        uiLogic.initWallet(
            AppDatabase.getInstance(ctx),
            ErgoApiService.getOrInit(Preferences(ctx)),
            walletId,
            derivationIdx,
            paymentRequest
        )
    }

    inner class AndroidSendFundsUiLogic : SendFundsUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun notifyWalletStateLoaded() {
            wallet?.walletConfig?.displayName?.let {
                _walletName.postValue(it)
            }
        }

        override fun notifyDerivedAddressChanged() {
            _address.postValue(derivedAddress)
        }

        override fun notifyBalanceChanged() {
            _walletBalance.postValue(balance)
        }

        override fun notifyAmountsChanged() {
            _grossAmount.postValue(grossAmount)
        }

        override fun notifyTokensChosenChanged() {
            _tokensChosenLiveData.postValue(tokensChosen.keys.toList())
        }

        override fun notifyUiLocked(locked: Boolean) {
            _lockInterface.postValue(locked)
        }

        override fun notifyHasTxId(txId: String) {
            _txId.postValue(txId)
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            _txWorkDoneLiveData.postValue(txResult)
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            _signingPromptData.postValue(signingPrompt)
        }

        override fun showErrorMessage(message: String) {
            _errorMessageLiveData.postValue(message)
        }
    }
}