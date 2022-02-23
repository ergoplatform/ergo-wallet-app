package org.ergoplatform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.api.TokenPriceApi
import org.ergoplatform.api.coingecko.CoinGeckoApi
import org.ergoplatform.api.ergodex.ErgoDexPriceApi
import org.ergoplatform.persistance.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.addresses.ensureWalletAddressListHasFirstAddress
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * fetches Wallet balances and fiat values
 */
class WalletStateSyncManager {

    val isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val singleAddressRefresh: MutableStateFlow<Long> = MutableStateFlow(0)
    val fiatValue: MutableStateFlow<Float> = MutableStateFlow(0f)
    val currencies: MutableStateFlow<List<String>?> = MutableStateFlow(null)
    var lastRefreshMs: Long = 0
        private set
    var lastHadError: Boolean = false
        private set
    var fiatCurrency: String = ""
        private set
    private val coinGeckoApi: CoinGeckoApi

    val tokenPrices: HashMap<String, TokenPrice> = HashMap()
    private val TOKEN_PRICE_REFRESH_DURATION_MS = 1000L * 60
    private var lastTokenPriceRefreshMs: Long = 0
    private val tokenPriceApi: TokenPriceApi = ErgoDexPriceApi()

    init {
        val retrofitCoinGecko = Retrofit.Builder().baseUrl("https://api.coingecko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        coinGeckoApi = retrofitCoinGecko.create(CoinGeckoApi::class.java)
    }

    fun invalidateCache(resetFiatValue: Boolean = false) {
        lastRefreshMs = 0
        if (resetFiatValue) fiatValue.value = 0.0f
    }


    fun refreshByUser(preferences: PreferencesProvider, database: IAppDatabase): Boolean {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 10) {
            refreshNow(preferences, database)
            return true
        } else
            return false
    }

    fun refreshWhenNeeded(preferences: PreferencesProvider, database: IAppDatabase) {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 60) {
            refreshNow(preferences, database)
        }
    }

    private fun refreshNow(preferences: PreferencesProvider, database: IAppDatabase) {
        if (!(isRefreshing.value)) {
            isRefreshing.value = true
            GlobalScope.launch(Dispatchers.IO) {
                var hadError = false
                var didSync = false

                val refreshFiatValueJob = launch {
                    // Refresh Ergo fiat value
                    refreshErgFiatValue(preferences)
                }

                val refreshTokenPriceJob =
                    if (System.currentTimeMillis() - lastTokenPriceRefreshMs > TOKEN_PRICE_REFRESH_DURATION_MS)
                        launch { refreshTokenPrices(database.tokenDbProvider) }
                    else null

                // Refresh wallet states
                try {
                    val statesSaved = refreshWalletStates(preferences, database.walletDbProvider)
                    didSync = statesSaved.isNotEmpty()
                } catch (t: Throwable) {
                    LogUtils.logDebug("NodeConnector", "Error: " + t.message, t)
                    t.printStackTrace()
                    // TODO report to user
                    hadError = true
                }

                refreshFiatValueJob.join()
                refreshTokenPriceJob?.join()

                if (!hadError && didSync) {
                    lastRefreshMs = System.currentTimeMillis()
                    preferences.lastRefreshMs = lastRefreshMs
                }
                LogUtils.logDebug("NodeConnector", "Refresh done, errors: $hadError")
                lastHadError = hadError
                isRefreshing.value = false
            }
        }
    }

    private suspend fun refreshTokenPrices(tokenDbProvider: TokenDbProvider) {
        try {
            val tokensFromPriceApi = tokenPriceApi.getTokenPrices()

            tokensFromPriceApi?.let {
                fillTokenPriceHashMap(it)
                lastTokenPriceRefreshMs = System.currentTimeMillis()
                tokenDbProvider.updateTokenPrices(it)
            }
        } catch (t: Throwable) {
            LogUtils.logDebug("TokenPrices", "Error: " + t.message, t)
        }
    }

    private fun fillTokenPriceHashMap(tokenPrices: List<TokenPrice>) {
        synchronized(this.tokenPrices) {
            this.tokenPrices.clear()
            tokenPrices.forEach {
                this.tokenPrices[it.tokenId] = it
            }
        }
    }

    private fun refreshErgFiatValue(preferences: PreferencesProvider) {
        fiatCurrency = preferences.prefDisplayCurrency

        var fFiatValue = fiatValue.value
        if (fiatCurrency.isNotEmpty()) {
            LogUtils.logDebug("NodeConnector", "Refresh fiat value")
            try {
                val currencyGetPrice =
                    coinGeckoApi.currencyGetPrice(fiatCurrency).execute().body()
                fFiatValue = currencyGetPrice?.ergoPrice?.get(fiatCurrency) ?: 0f
            } catch (t: Throwable) {
                LogUtils.logDebug("NodeConnector", "Error: " + t.message, t)
                // don't set to zero here, keep last value in case of connection error
            }
        } else {
            fFiatValue = 0f
        }
        preferences.lastFiatValue = fFiatValue
        fiatValue.value = fFiatValue
    }

    fun refreshSingleAddresses(
        preferences: PreferencesProvider,
        database: WalletDbProvider,
        addresses: List<String>
    ) {
        if (addresses.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val statesSaved = refreshWalletStates(preferences, database, addresses)
                    if (statesSaved.isNotEmpty()) {
                        singleAddressRefresh.value = System.currentTimeMillis()
                    }
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
                        ErgoApiService.getOrInit(preferences).getTotalBalanceForAddress(
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
            LogUtils.logDebug(
                "NodeConnector",
                "Persisting ${statesToSave.size} wallet states to db"
            )
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

    fun loadPreferenceValues(preferences: PreferencesProvider, appDatabase: IAppDatabase) {
        lastRefreshMs = preferences.lastRefreshMs
        fiatCurrency = preferences.prefDisplayCurrency
        fiatValue.value = preferences.lastFiatValue
        LogUtils.logDebug("NodeConnector", "Initialized preferences.")

        GlobalScope.launch {
            fillTokenPriceHashMap(appDatabase.tokenDbProvider.loadTokenPrices())
        }
    }

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: WalletStateSyncManager? = null

        fun getInstance(): WalletStateSyncManager {
            return instance ?: synchronized(this) {
                instance ?: WalletStateSyncManager().also { instance = it }
            }
        }

    }
}