package org.ergoplatform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.api.CoinGeckoApi
import org.ergoplatform.explorer.client.DefaultApi
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.persistance.WalletState
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.wallet.addresses.ensureWalletAddressListHasFirstAddress
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class NodeConnector {

    val isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val fiatValue: MutableStateFlow<Float> = MutableStateFlow(0f)
    val currencies: MutableStateFlow<List<String>?> = MutableStateFlow(null)
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

    private fun getOrInitErgoApiService(preferences: PreferencesProvider): DefaultApi {
        if (ergoApiService == null) {

            val retrofit = Retrofit.Builder()
                .baseUrl(preferences.prefExplorerApiUrl)
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

    fun refreshByUser(preferences: PreferencesProvider, database: WalletDbProvider): Boolean {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 10) {
            refreshNow(preferences, database)
            return true
        } else
            return false
    }

    fun refreshWhenNeeded(preferences: PreferencesProvider, database: WalletDbProvider) {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 60) {
            refreshNow(preferences, database)
        }
    }

    private fun refreshNow(preferences: PreferencesProvider, database: WalletDbProvider) {
        if (!(isRefreshing.value)) {
            isRefreshing.value = true
            GlobalScope.launch(Dispatchers.IO) {
                var hadError = false
                var didSync = false

                // Refresh Ergo fiat value
                fiatCurrency = preferences.prefDisplayCurrency

                var fFiatValue = fiatValue.value
                if (fiatCurrency.isNotEmpty()) {
                    try {
                        val currencyGetPrice =
                            coinGeckoApi.currencyGetPrice(fiatCurrency).execute().body()
                        fFiatValue = currencyGetPrice?.ergoPrice?.get(fiatCurrency) ?: 0f
                    } catch (t: Throwable) {
                        // don't set to zero here, keep last value in case of connection error
                    }
                } else {
                    fFiatValue = 0f
                }
                preferences.lastFiatValue = fFiatValue
                fiatValue.value = fFiatValue


                // Refresh wallet states
                try {
                    val statesSaved = refreshWalletStates(preferences, database)
                    didSync = statesSaved.isNotEmpty()
                } catch (t: Throwable) {
                    // TODO report to user
                    hadError = true
                }

                if (!hadError && didSync) {
                    lastRefreshMs = System.currentTimeMillis()
                    preferences.lastRefreshMs = lastRefreshMs
                }
                lastHadError = hadError
                isRefreshing.value = false
            }
        }
    }

    fun refreshSingleAddresses(
        preferences: PreferencesProvider,
        database: WalletDbProvider,
        addresses: List<String>
    ) {
        if (addresses.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    refreshWalletStates(preferences, database, addresses)
                } catch (t: Throwable) {
                    // ignore the error for a single address call
                }
            }
        }
    }

    private suspend fun refreshWalletStates(
        preferences: PreferencesProvider,
        database: WalletDbProvider,
        addressFilter: List<String> = emptyList()
    ): List<WalletState> {
        val statesToSave = mutableListOf<WalletState>()
        val tokenAddressesToDelete = mutableListOf<String>()
        val tokensToSave = mutableListOf<WalletToken>()
        database.getAllWalletConfigsSynchronous().forEach { walletConfig ->
            walletConfig.firstAddress?.let { firstAddress ->
                val allAddresses = ensureWalletAddressListHasFirstAddress(
                    database.loadWalletAddresses(firstAddress), firstAddress
                )

                val refreshAddresses =
                    if (addressFilter.isEmpty()) allAddresses
                    else allAddresses.filter { addressFilter.contains(it.publicAddress) }

                refreshAddresses.forEach { address ->
                    val balanceInfoCall =
                        getOrInitErgoApiService(preferences).getApiV1AddressesP1BalanceTotal(
                            address.publicAddress
                        ).execute()

                    balanceInfoCall.body()?.let { balanceInfo ->

                        val newState = WalletState(
                            address.publicAddress,
                            address.walletFirstAddress,
                            balanceInfo.confirmed?.nanoErgs,
                            balanceInfo.unconfirmed?.nanoErgs
                        )

                        statesToSave.add(newState)
                        tokenAddressesToDelete.add(address.publicAddress)
                        balanceInfo.confirmed?.tokens?.forEach {
                            tokensToSave.add(
                                WalletToken(
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
        }

        database.withTransaction {
            database.insertWalletStates(statesToSave)
            tokenAddressesToDelete.forEach { database.deleteTokensByAddress(it) }
            database.insertWalletTokens(tokensToSave)
        }
        return statesToSave
    }

    fun fetchCurrencies() {
        // do this only once per session, won't change often
        if (currencies.value == null || currencies.value!!.isEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    currencies.value = null
                    val currencyList = coinGeckoApi.currencies.execute().body()
                    currencies.value = currencyList ?: emptyList()
                } catch (t: Throwable) {
                    currencies.value = emptyList()
                }
            }
        }
    }

    fun loadPreferenceValues(preferences: PreferencesProvider) {
        lastRefreshMs = preferences.lastRefreshMs
        fiatCurrency = preferences.prefDisplayCurrency
        fiatValue.value = preferences.lastFiatValue
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