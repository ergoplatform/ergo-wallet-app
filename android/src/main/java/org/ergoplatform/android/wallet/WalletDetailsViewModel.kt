package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic
import org.ergoplatform.wallet.getDerivedAddress
import org.ergoplatform.wallet.getNumOfAddresses

class WalletDetailsViewModel : ViewModel() {

    val uiLogic = WalletDetailsUiLogic()
    val wallet: Wallet? get() = uiLogic.wallet

    // the selected index is null for "all addresses"
    var selectedIdx: Int?
        get() = uiLogic.addressIdx
        set(value) {
            uiLogic.addressIdx = value
            notifyObserversDerivedIdxChanged()
        }

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?> = _address

    fun init(ctx: Context, walletId: Int) {
        viewModelScope.launch {
            AppDatabase.getInstance(ctx).walletDao().walletWithStateByIdAsFlow(walletId).collect {
                // called every time something changes in the DB
                uiLogic.wallet = it?.toModel()

                // no address set (yet) and there is only a single address available, fix it to this one
                if (selectedIdx == null && wallet?.getNumOfAddresses() == 1) {
                    selectedIdx = 0
                } else {
                    // make sure to post to observer the first time or on DB change
                    notifyObserversDerivedIdxChanged()
                }
            }
        }
    }

    private fun notifyObserversDerivedIdxChanged() {
        _address.postValue(selectedIdx?.let { wallet?.getDerivedAddress(it) })
    }
}