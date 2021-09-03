package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase

class WalletAddressesViewModel : ViewModel() {
    var wallet: WalletDbEntity? = null
        private set

    private val _addresses = MutableLiveData<List<WalletAddressDbEntity>>()
    val addresses: LiveData<List<WalletAddressDbEntity>> = _addresses

    fun init(ctx: Context, walletId: Int) {
        viewModelScope.launch {
            AppDatabase.getInstance(ctx).walletDao().walletWithStateByIdAsFlow(walletId).collect {
                // called every time something changes in the DB
                wallet = it

                _addresses.postValue(it.getSortedDerivedAddressesList())
            }
        }
    }
}