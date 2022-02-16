package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
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
    protected val _signingPromptData = MutableLiveData<String?>()
    val signingPromptData: LiveData<String?> = _signingPromptData

    fun startPaymentWithPassword(password: String, context: Context): Boolean {
        uiLogic.wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: SigningSecrets?
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                mnemonic = SigningSecrets.fromJson(String(decryptData!!))
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

            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            val mnemonic = SigningSecrets.fromJson(String(decryptData!!))

            uiLogic.startPaymentWithMnemonicAsync(
                mnemonic!!,
                Preferences(context),
                AndroidStringProvider(context)
            )

        }
    }
}