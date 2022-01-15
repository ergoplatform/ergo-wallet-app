package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ErgoPaySigningRequest
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic

/**
 * Holding state of the send funds screen (thus to be expected to get complicated)
 */
class SendFundsViewModel : ViewModel() {
    val uiLogic = AndroidSendFundsUiLogic()

    private val _lockInterface = MutableLiveData<Boolean>()
    val lockInterface: LiveData<Boolean> = _lockInterface
    private val _walletName = MutableLiveData<String>()
    val walletName: LiveData<String> = _walletName
    private val _address = MutableLiveData<WalletAddress?>()
    val address: LiveData<WalletAddress?> = _address
    private val _walletBalance = MutableLiveData<ErgoAmount>()
    val walletBalance: LiveData<ErgoAmount> = _walletBalance
    private val _grossAmount = MutableLiveData<ErgoAmount>().apply {
        value = ErgoAmount.ZERO
    }
    val grossAmount: LiveData<ErgoAmount> = _grossAmount
    private val _txWorkDoneLiveData = SingleLiveEvent<TransactionResult>()
    val txWorkDoneLiveData: LiveData<TransactionResult> = _txWorkDoneLiveData
    private val _txId = MutableLiveData<String?>()
    val txId: LiveData<String?> = _txId
    private val _signingPromptData = MutableLiveData<String?>()
    val signingPromptData: LiveData<String?> = _signingPromptData

    // the live data gets data posted on adding or removing tokens, not on every amount change
    private val _tokensChosenLiveData = MutableLiveData<List<String>>()
    val tokensChosenLiveData: LiveData<List<String>> = _tokensChosenLiveData

    private val _errorMessageLiveData = SingleLiveEvent<String>()
    val errorMessageLiveData: LiveData<String> = _errorMessageLiveData

    fun initWallet(ctx: Context, walletId: Int, derivationIdx: Int, paymentRequest: String?) {
        uiLogic.initWallet(
            RoomWalletDbProvider(AppDatabase.getInstance(ctx)),
            walletId,
            derivationIdx,
            paymentRequest
        )
    }

    fun startPaymentWithPassword(password: String, context: Context): Boolean {
        uiLogic.wallet?.walletConfig?.secretStorage?.let {
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

            uiLogic.startPaymentWithMnemonicAsync(
                mnemonic,
                Preferences(context),
                AndroidStringProvider(context)
            )

            return true
        }

        return false
    }

    fun startPaymentUserAuth(context: Context) {
        // we don't handle exceptions here by intention: we throw them back to the fragment which
        // will show a snackbar to give the user a hint what went wrong
        uiLogic.wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: String?

            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            mnemonic = deserializeSecrets(String(decryptData!!))

            uiLogic.startPaymentWithMnemonicAsync(
                mnemonic!!,
                Preferences(context),
                AndroidStringProvider(context)
            )

        }
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