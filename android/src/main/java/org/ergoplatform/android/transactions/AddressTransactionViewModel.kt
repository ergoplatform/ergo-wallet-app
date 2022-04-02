package org.ergoplatform.android.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.launch
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.TransactionDbProvider
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.wallet.getDerivedAddress
import org.ergoplatform.wallet.getDerivedAddressEntity

class AddressTransactionViewModel : ViewModel() {

    private var wallet: Wallet? = null
    private val _wallet = MutableLiveData<Wallet?>()
    val walletLiveData: LiveData<Wallet?> get() = _wallet

    var derivationIdx = 0

    val derivedAddress get() = wallet?.getDerivedAddressEntity(derivationIdx)

    fun init(walletId: Int, derivationIdx: Int, db: IAppDatabase) {
        if (wallet == null) {
            this.derivationIdx = derivationIdx
            viewModelScope.launch {
                wallet = db.walletDbProvider.loadWalletWithStateById(walletId)
                _wallet.postValue(wallet)
            }
        }
    }

    fun getDataFlow(transactionDbProvider: TransactionDbProvider) = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        TransactionPagedSource(
            transactionDbProvider,
            wallet?.getDerivedAddress(derivationIdx) ?: ""
        )
    }.flow


    class TransactionPagedSource(
        private val transactionDbProvider: TransactionDbProvider,
        private val address: String
    ) :
        PagingSource<Int, AddressTransactionWithTokens>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AddressTransactionWithTokens> {
            // Start refresh at page 1 if undefined.
            val nextPageNumber = params.key ?: 0
            val response =
                transactionDbProvider.loadAddressTransactionsWithTokens(address, 50, nextPageNumber)
            return LoadResult.Page(
                data = response,
                prevKey = null, // Only paging forward
                nextKey = if (response.isEmpty()) null else nextPageNumber + 1
            )
        }

        override fun getRefreshKey(state: PagingState<Int, AddressTransactionWithTokens>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }

        }
    }
}