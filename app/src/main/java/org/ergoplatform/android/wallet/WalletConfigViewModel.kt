package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.deserializeSecrets
import org.ergoplatform.android.ui.PasswordDialogFragment
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.api.AesEncryptionManager

class WalletConfigViewModel : ViewModel() {
    private val _snackbarEvent = SingleLiveEvent<Int>()
    val snackbarEvent: LiveData<Int> = _snackbarEvent

    private var wallet: WalletConfigDbEntity? = null

    fun saveChanges(context: Context, walletId: Int, newWalletName: String?) {
        viewModelScope.launch {
            val walletDao = AppDatabase.getInstance(context).walletDao()
            val wallet = walletDao.loadWalletById(walletId)

            wallet?.let {
                val newWalletConfig = WalletConfigDbEntity(
                    it.id,
                    newWalletName ?: it.displayName,
                    it.firstAddress,
                    it.encryptionType,
                    it.secretStorage
                )

                walletDao.update(newWalletConfig)
                _snackbarEvent.postValue(R.string.label_changes_saved)
            }
        }
    }

    fun deleteWallet(context: Context, walletId: Int) {
        // GlobalScope to let deletion process when fragment is already dismissed
        GlobalScope.launch {
            val database = AppDatabase.getInstance(context)
            val walletDao = database.walletDao()
            val walletConfig = walletDao.loadWalletById(walletId)
            walletConfig?.let {
                database.withTransaction {
                    walletConfig.firstAddress?.let { firstAddress ->
                        walletDao.deleteWalletState(firstAddress)
                        walletDao.deleteTokensByWallet(firstAddress)
                        walletDao.deleteWalletAddresses(firstAddress)
                    }
                    walletDao.deleteWalletConfig(walletId)
                }

                // After we deleted a wallet, we can prune the keystore if it is not needed
                if (walletDao.getAllSync().filter { it.encryptionType == ENC_TYPE_DEVICE }
                        .isEmpty()) {
                    AesEncryptionManager.emptyKeystore()
                }
            }
        }
    }

    fun prepareDisplayMnemonic(fragment: WalletConfigFragment, walletId: Int) {
        viewModelScope.launch {
            val walletDao = AppDatabase.getInstance(fragment.requireContext()).walletDao()
            wallet = walletDao.loadWalletById(walletId)

            wallet?.let {
                if (it.encryptionType == ENC_TYPE_PASSWORD) {
                    PasswordDialogFragment().show(
                        fragment.childFragmentManager,
                        null
                    )
                } else if (it.encryptionType == ENC_TYPE_DEVICE) {
                    fragment.showBiometricPrompt()
                }
            }
        }

    }

    fun decryptMnemonicWithPass(password: String): String? {
        wallet?.secretStorage?.let {
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                return deserializeSecrets(String(decryptData!!))
            } catch (t: Throwable) {
                // Password wrong
                return null
            }

        }
        return null
    }

    fun decryptMnemonicWithUserAuth(): String? {
        wallet?.secretStorage?.let {
            val decryptData = AesEncryptionManager.decryptDataWithDeviceKey(it)
            return deserializeSecrets(String(decryptData!!))
        }
        return null
    }
}