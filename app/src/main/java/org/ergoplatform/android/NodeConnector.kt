package org.ergoplatform.android

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.wallet.WalletStateDbEntity
import org.ergoplatform.android.wallet.WalletTokenDbEntity
import org.ergoplatform.android.wallet.ensureWalletAddressListHasFirstAddress
import org.ergoplatform.api.CoinGeckoApi
import org.ergoplatform.explorer.client.DefaultApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class NodeConnector {

    val isRefreshing: MutableLiveData<Boolean> = MutableLiveData()
    val refreshNum: MutableLiveData<Int> = MutableLiveData()
    val fiatValue: MutableLiveData<Float> = MutableLiveData()
    val currencies: MutableLiveData<List<String>?> = MutableLiveData()
    var lastRefreshMs: Long = 0
        private set
    var lastHadError: Boolean = false
        private set
    var fiatCurrency: String = ""
        private set
    private var ergoApiService: DefaultApi? = null
    private val coinGeckoApi: CoinGeckoApi

    init {
        val retrofitCoinGecko = Retrofit.Builder().baseUrl("https://api.coingecko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        coinGeckoApi = retrofitCoinGecko.create(CoinGeckoApi::class.java)
    }

    private fun getOrInitErgoApiService(context: Context): DefaultApi {
        if (ergoApiService == null) {

            val retrofit = Retrofit.Builder()
                .baseUrl(getPrefExplorerApiUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            ergoApiService = retrofit.create(DefaultApi::class.java)
        }
        return ergoApiService!!
    }

    fun invalidateCache() {
        lastRefreshMs = 0
    }

    fun resetApiService() {
        ergoApiService = null
    }

    fun refreshByUser(context: Context): Boolean {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 10) {
            refreshNow(context)
            return true
        } else
            return false
    }

    fun refreshWhenNeeded(context: Context) {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 60) {
            refreshNow(context)
        }
    }

    private fun refreshNow(context: Context) {
        if (!(isRefreshing.value ?: false)) {
            isRefreshing.postValue(true)
            GlobalScope.launch(Dispatchers.IO) {
                var hadError = false
                var didSync = false

                // Refresh Ergo fiat value
                fiatCurrency = getPrefDisplayCurrency(context)

                var fFiatValue = fiatValue.value ?: 0f
                if (fiatCurrency.isNotEmpty()) {
                    try {
                        val currencyGetPrice =
                            coinGeckoApi.currencyGetPrice(fiatCurrency).execute().body()
                        fFiatValue = currencyGetPrice?.ergoPrice?.get(fiatCurrency) ?: 0f
                    } catch (t: Throwable) {
                        Log.e("CoinGecko", "Error", t)
                        // don't set to zero here, keep last value in case of connection error
                    }
                } else {
                    fFiatValue = 0f
                }
                saveLastFiatValue(context, fFiatValue)
                fiatValue.postValue(fFiatValue)


                // Refresh wallet states
                try {
                    val statesSaved = refreshWalletStates(context)
                    didSync = statesSaved.isNotEmpty()
                } catch (t: Throwable) {
                    Log.e("NodeConnector", "Error", t)
                    // TODO report to user
                    hadError = true
                }

                if (!hadError && didSync) {
                    lastRefreshMs = System.currentTimeMillis()
                    saveLastRefreshMs(context, lastRefreshMs)
                }
                lastHadError = hadError
                refreshNum.postValue(refreshNum.value?.and(1) ?: 0)
                isRefreshing.postValue(false)
            }
        }
    }

    private suspend fun refreshWalletStates(context: Context): List<WalletStateDbEntity> {
        val statesToSave = mutableListOf<WalletStateDbEntity>()
        val tokenAddressesToDelete = mutableListOf<String>()
        val tokensToSave = mutableListOf<WalletTokenDbEntity>()
        val database = AppDatabase.getInstance(context)
        val walletDao = database.walletDao()
        walletDao.getAllWalletConfigsSyncronous().forEach { walletConfig ->
            walletConfig.firstAddress?.let { firstAddress ->
                val allAddresses = ensureWalletAddressListHasFirstAddress(
                    walletDao.loadWalletAddresses(firstAddress), firstAddress
                )

                allAddresses.forEach { address ->
                    val balanceInfo =
                        getOrInitErgoApiService(context).getApiV1AddressesP1BalanceTotal(
                            address.publicAddress
                        ).execute().body()

                    val newState = WalletStateDbEntity(
                        address.publicAddress,
                        address.walletFirstAddress,
                        balanceInfo?.confirmed?.nanoErgs,
                        balanceInfo?.unconfirmed?.nanoErgs
                    )

                    statesToSave.add(newState)
                    tokenAddressesToDelete.add(address.publicAddress)
                    balanceInfo?.confirmed?.tokens?.forEach {
                        tokensToSave.add(
                            WalletTokenDbEntity(
                                0,
                                address.publicAddress,
                                address.walletFirstAddress,
                                it.tokenId,
                                it.amount,
                                it.decimals,
                                it.name
                            )
                        )
                    }

                }
            }
        }

        database.withTransaction {
            walletDao.insertWalletStates(*statesToSave.toTypedArray())
            tokenAddressesToDelete.forEach { walletDao.deleteTokensByAddress(it) }
            walletDao.insertWalletTokens(*tokensToSave.toTypedArray())
        }
        return statesToSave
    }

    fun fetchCurrencies() {
        // do this only once per session, won't change often
        if (currencies.value == null || currencies.value!!.isEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    currencies.postValue(null)
                    val currencyList = coinGeckoApi.currencies.execute().body()
                    currencies.postValue(currencyList ?: emptyList())
                } catch (t: Throwable) {
                    Log.e("CoinGecko", "Error", t)
                    currencies.postValue(emptyList())
                }
            }
        }
    }

    fun loadPreferenceValues(context: Context) {
        lastRefreshMs = getLastRefreshMs(context)
        fiatCurrency = getPrefDisplayCurrency(context)
        fiatValue.postValue(getLastFiatValue(context))
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: NodeConnector? = null

        fun getInstance(): NodeConnector {
            return instance ?: synchronized(this) {
                instance ?: NodeConnector().also { instance = it }
            }
        }

    }
}