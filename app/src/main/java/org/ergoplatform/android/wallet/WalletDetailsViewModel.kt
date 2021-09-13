package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase

class WalletDetailsViewModel : ViewModel() {

    var wallet: WalletDbEntity? = null
        private set

    // the selected index is null for "all addresses"
    var selectedIdx: Int? = null
        set(value) {
            field = value
            _address.postValue(selectedIdx?.let { wallet?.getDerivedAddress(it) })
        }

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?> = _address

    fun init(ctx: Context, walletId: Int) {
        viewModelScope.launch {
            AppDatabase.getInstance(ctx).walletDao().walletWithStateByIdAsFlow(walletId).collect {
                // called every time something changes in the DB
                wallet = it

                // if there is only a single address available, fix it to this one
                if (it.getNumOfAddresses() == 1) {
                    selectedIdx = 0
                } else {
                    selectedIdx = null
                }
            }
        }
    }
}