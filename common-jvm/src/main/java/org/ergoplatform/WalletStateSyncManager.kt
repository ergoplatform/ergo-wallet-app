package org.ergoplatform

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.api.TokenPriceApi
import org.ergoplatform.api.coingecko.CoinGeckoApi
import org.ergoplatform.api.ergodex.ErgoDexPriceApi
import org.ergoplatform.api.tokenjay.TokenJayPriceApi
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
    val hasFiatValue get() = fiatValue.value > 0f && fiatCurrency.isNotEmpty()
    private val coinGeckoApi: CoinGeckoApi

    private val tokenPrices: HashMap<String, TokenPrice> = HashMap()
    private val tokenPriceRefreshDurationMs = 1000L * 60
    private var lastTokenPriceRefreshMs: Long = 0
    private val tokenPriceSources: List<TokenPriceApi> = listOf(
        ErgoDexPriceApi(),
        TokenJayPriceApi()
    )

    init {
        val retrofitCoinGecko = Retrofit.Builder().baseUrl("https://api.coingecko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpSingleton.getInstance())
            .build()
        coinGeckoApi = retrofitCoinGecko.create(CoinGeckoApi::class.java)
    }

    fun invalidateCache(resetFiatValue: Boolean = false) {
        lastRefreshMs = 0
        if (resetFiatValue) fiatValue.value = 0.0f
    }

    fun getTokenPrice(tokenId: String?): TokenPrice? {
        return tokenId?.let {
            synchronized(tokenPrices) {
                tokenPrices[tokenId]
            }
        }
    }

    fun refreshByUser(preferences: PreferencesProvider, database: IAppDatabase): Boolean {
        return if (System.currentTimeMillis() - lastRefreshMs > 1000L * 10) {
            refreshNow(preferences, database)
            true
        } else
            false
    }

    fun refreshWhenNeeded(preferences: PreferencesProvider, database: IAppDatabase) {
        if (System.currentTimeMillis() - lastRefreshMs > 1000L * 60) {
            refreshNow(preferences, database)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
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
                    if (System.currentTimeMillis() - lastTokenPriceRefreshMs > tokenPriceRefreshDurationMs)
                        launch { refreshTokenPrices(database.tokenDbProvider) }
                    else null

                // Refresh wallet states
                try {
                    val statesSaved = refreshWalletStates(preferences, database.walletDbProvider)
                    didSync = statesSaved.isNotEmpty()
                } catch (t: Throwable) {
                    LogUtils.logDebug(
                        this.javaClass.simpleName,
                        "refreshWalletStates error: " + t.message,
                        t
                    )
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
                LogUtils.logDebug(this.javaClass.simpleName, "Refresh done, errors: $hadError")
                lastHadError = hadError
                isRefreshing.value = false
            }
        }
    }

    private suspend fun refreshTokenPrices(tokenDbProvider: TokenDbProvider) {
        val tokenPriceList = tokenPriceSources.map { tokenPriceApi ->
            try {
                tokenPriceApi.getTokenPrices()
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "refreshTokenPrices error: " + t.message,
                    t
                )
                null
            } ?: emptyList()
        }.flatten().sortedBy { it.second.ordinal }.map { it.first }

        if (tokenPriceList.isNotEmpty()) {
            val pricesInMap = fillTokenPriceHashMap(tokenPriceList)
            lastTokenPriceRefreshMs = System.currentTimeMillis()
            tokenDbProvider.updateTokenPrices(pricesInMap.toList())
        }
    }

    private fun fillTokenPriceHashMap(tokenPrices: List<TokenPrice>): Collection<TokenPrice> {
        return synchronized(this.tokenPrices) {
            this.tokenPrices.clear()
            tokenPrices.forEach {
                this.tokenPrices[it.tokenId] = it
            }
            this.tokenPrices.values
        }
    }

    private fun refreshErgFiatValue(preferences: PreferencesProvider) {
        fiatCurrency = preferences.prefDisplayCurrency

        var fFiatValue = fiatValue.value
        if (fiatCurrency.isNotEmpty()) {
            LogUtils.logDebug(this.javaClass.simpleName, "Refresh fiat value")
            try {
                val currencyGetPrice =
                    coinGeckoApi.currencyGetPrice(fiatCurrency).execute().body()
                fFiatValue = currencyGetPrice?.ergoPrice?.get(fiatCurrency) ?: 0f
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "refreshErgFiatValue error: " + t.message,
                    t
                )
                // don't set to zero here, keep last value in case of connection error
            }
        } else {
            fFiatValue = 0f
        }
        preferences.lastFiatValue = fFiatValue
        fiatValue.value = fFiatValue
    }

    @OptIn(DelicateCoroutinesApi::class)
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
                        ApiServiceManager.getOrInit(preferences).getTotalBalanceForAddress(
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
                this.javaClass.simpleName,
                "Persisting ${statesToSave.size} wallet states to db"
            )
            database.insertWalletStates(statesToSave)
            tokenAddressesToDelete.forEach { database.deleteTokensByAddress(it) }
            database.insertWalletTokens(tokensToSave)
        }
        return statesToSave
    }

    @OptIn(DelicateCoroutinesApi::class)
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

    @OptIn(DelicateCoroutinesApi::class)
    fun loadPreferenceValues(preferences: PreferencesProvider, appDatabase: IAppDatabase) {
        lastRefreshMs = preferences.lastRefreshMs
        fiatCurrency = preferences.prefDisplayCurrency
        fiatValue.value = preferences.lastFiatValue
        LogUtils.logDebug(this.javaClass.simpleName, "Initialized preferences.")

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