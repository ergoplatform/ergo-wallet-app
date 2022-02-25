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

    val uiLogic = AndroidDetailsUiLogic()
    val wallet: Wallet? get() = uiLogic.wallet

    // the selected index is null for "all addresses"
    var selectedIdx: Int?
        get() = uiLogic.addressIdx
        set(value) {
            uiLogic.setAddressIdx(value)
        }

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?> = _address

    fun init(ctx: Context, walletId: Int) {
        viewModelScope.launch {
            uiLogic.setUpWalletStateFlowCollector(AppDatabase.getInstance(ctx).walletDbProvider, walletId)
        }
    }

    inner class AndroidDetailsUiLogic: WalletDetailsUiLogic() {
        override fun onDataChanged() {
            _address.postValue(selectedIdx?.let { wallet?.getDerivedAddress(it) })
        }
    }
}