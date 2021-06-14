package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.ui.SingleLiveEvent

class WalletConfigViewModel : ViewModel() {
    private val _snackbarEvent = SingleLiveEvent<Int>()
    val snackbarEvent: LiveData<Int> = _snackbarEvent

    fun saveChanges(context: Context, walletId: Int, newWalletName: String?) {
        viewModelScope.launch {
            val walletDao = AppDatabase.getInstance(context).walletDao()
            val wallet = walletDao.loadWalletById(walletId)

            wallet?.let {
                val newWalletConfig = WalletConfigDbEntity(
                    it.id,
                    newWalletName ?: it.displayName,
                    it.publicAddress,
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
            val walletDao = AppDatabase.getInstance(context).walletDao()
            walletDao.deleteWalletState(walletId)
            walletDao.deleteWalletConfig(walletId)
        }
    }
}