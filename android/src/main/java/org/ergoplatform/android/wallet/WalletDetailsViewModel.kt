package org.ergoplatform.android.wallet

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic
import org.ergoplatform.wallet.getDerivedAddress

class WalletDetailsViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    val addressIdxStateKey = "KEY_ADDRESS_IDX"

    val uiLogic = AndroidDetailsUiLogic()
    val wallet: Wallet? get() = uiLogic.wallet

    // the selected index is null for "all addresses"
    val selectedIdx get() = uiLogic.addressIdx

    private val _address = MutableLiveData<String?>()
    val address: LiveData<String?> = _address
    private val _tokenInfo = MutableLiveData<HashMap<String, TokenInformation>>()
    val tokenInfo: LiveData<HashMap<String, TokenInformation>> = _tokenInfo

    val infoVersion = mutableStateOf(0)

    fun init(ctx: Context, walletId: Int) {
        uiLogic.setUpWalletStateFlowCollector(
            AppDatabase.getInstance(ctx),
            walletId,
            savedState[addressIdxStateKey]
        )
    }

    inner class AndroidDetailsUiLogic : WalletDetailsUiLogic() {
        override val coroutineScope: CoroutineScope get() = viewModelScope

        override fun onDataChanged() {
            savedState[addressIdxStateKey] = selectedIdx
            _address.postValue(selectedIdx?.let { wallet?.getDerivedAddress(it) })
            infoVersion.value = infoVersion.value + 1
        }

        override fun onNewTokenInfoGathered(tokenInformation: TokenInformation) {
            // cause a UI refresh for tokens
            _tokenInfo.postValue(this.tokenInformation)
            infoVersion.value = infoVersion.value + 1
        }
    }
}