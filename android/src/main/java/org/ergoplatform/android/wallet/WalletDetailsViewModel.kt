package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic
import org.ergoplatform.wallet.getDerivedAddress

class WalletDetailsViewModel : ViewModel() {

    val uiLogic = AndroidDetailsUiLogic()
    val wallet: Wallet? get() = uiLogic.wallet

    // the selected index is null for "all addresses"
    var selectedIdx: Int?
        get() = uiLogic.addressIdx
        set(value) {
            uiLogic.newAddressIdxChosen(value)
        }

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?> = _address
    private val _tokenInfo = MutableLiveData<HashMap<String, TokenInformation>>()
    val tokenInfo: LiveData<HashMap<String, TokenInformation>> = _tokenInfo

    fun init(ctx: Context, walletId: Int) {
        uiLogic.setUpWalletStateFlowCollector(
            AppDatabase.getInstance(ctx),
            walletId
        )
    }

    inner class AndroidDetailsUiLogic : WalletDetailsUiLogic() {
        override val coroutineScope: CoroutineScope get() = viewModelScope

        override fun onDataChanged() {
            _address.postValue(selectedIdx?.let { wallet?.getDerivedAddress(it) })
        }

        override fun onNewTokenInfoGathered(tokenInformation: TokenInformation) {
            // cause a UI refresh for tokens
            _tokenInfo.postValue(this.tokenInformation)
        }
    }
}