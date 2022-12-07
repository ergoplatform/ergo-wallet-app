package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ergoauth.isErgoAuthRequestUri
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.*
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.transactions.TransactionListManager
import org.ergoplatform.transactions.isColdSigningRequestChunk
import org.ergoplatform.transactions.isErgoAuthRequestChunk
import org.ergoplatform.transactions.isErgoPaySigningRequest
import org.ergoplatform.uilogic.STRING_ERROR_QR_CODE_CONTENT_UNKNOWN
import org.ergoplatform.uilogic.STRING_HINT_READONLY_SIGNING_REQUEST
import org.ergoplatform.uilogic.STRING_LABEL_ALL_ADDRESSES
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.wallet.*
import org.ergoplatform.wallet.addresses.getAddressLabel

abstract class WalletDetailsUiLogic {
    val maxTransactionsToShow = 5

    var wallet: Wallet? = null
        private set
    var addressIdx: Int? = null
        private set
    var walletAddress: WalletAddress? = null
        private set
    var tokensList: List<WalletToken> = emptyList()
        private set

    private var tokenInformationJob: Job? = null
    val tokenInformation: HashMap<String, TokenInformation> = HashMap()

    private var initialized = false

    abstract val coroutineScope: CoroutineScope

    fun setUpWalletStateFlowCollector(
        database: IAppDatabase,
        walletId: Int,
        addressIdx: Int? = null
    ) {
        if (initialized)
            return

        initialized = true

        addressIdx?.let { this.addressIdx = addressIdx }

        coroutineScope.launch {
            database.walletDbProvider.walletWithStateByIdAsFlow(walletId).collect {
                // called every time something changes in the DB
                onWalletStateChanged(it, database.tokenDbProvider)
            }
        }
    }

    /**
     * this needs to be public and callable from outside because on some platforms
     * walletWithStateByIdAsFlow does not cover all state changes, but only config changes
     */
    suspend fun onWalletStateChanged(it: Wallet?, tokenDbProvider: TokenDbProvider) {
        wallet = it

        // on first call, prefill the tokenInformation map with information right from the
        // db. This won't fetch or update existing information, this is done by
        // gatherTokenInformation after UI is drawn for the first time
        if (tokenInformation.isEmpty()) {
            wallet?.getTokensForAllAddresses()?.forEach { walletToken ->
                tokenDbProvider.loadTokenInformation(walletToken.tokenId!!)?.let {
                    synchronized(tokenInformation) {
                        tokenInformation[it.tokenId] = it
                    }
                }
            }
        }

        // no address set (yet) and there is only a single address available, fix it to this one
        if (addressIdx == null && wallet?.getNumOfAddresses() == 1) {
            addressIdx = 0
        }
        // make sure to post to observer the first time or on DB change
        refreshAddress()
    }

    fun newAddressIdxChosen(newAddressIdx: Int?, prefs: PreferencesProvider, db: IAppDatabase) {
        if (newAddressIdx != addressIdx) {
            addressIdx = newAddressIdx
            refreshAddress()
            refreshAddressTransactionsWhenNeeded(prefs, db)
        }
    }

    private fun refreshAddress() {
        walletAddress = addressIdx?.let { wallet?.getDerivedAddressEntity(it) }
        tokensList = (walletAddress?.let { wallet?.getTokensForAddress(it.publicAddress) }
            ?: wallet?.getTokensForAllAddresses() ?: emptyList()).sortedBy { it.name?.lowercase() }

        onDataChanged()
    }

    private fun refreshAddressTransactionsWhenNeeded(prefs: PreferencesProvider, db: IAppDatabase) {
        val addressesToRefresh = getSelectedAddresses()
        addressesToRefresh?.forEach {
            TransactionListManager.downloadTransactionListForAddress(
                it.publicAddress,
                ApiServiceManager.getOrInit(prefs),
                db
            )
        }
    }

    /**
     * called from UI when it became visible first time or after being in background
     */
    fun refreshWhenNeeded(
        prefs: PreferencesProvider,
        database: IAppDatabase,
        texts: StringProvider,
        rescheduleRefreshJob: (() -> Unit)?
    ) {
        WalletStateSyncManager.getInstance()
            .refreshWhenNeeded(prefs, database, texts, rescheduleRefreshJob)
        refreshAddressTransactionsWhenNeeded(prefs, database)
    }

    fun refreshByUser(
        prefs: PreferencesProvider,
        database: IAppDatabase,
        texts: StringProvider,
        rescheduleRefreshJob: (() -> Unit)?
    ): Boolean {
        refreshAddressTransactionsWhenNeeded(prefs, database)
        return WalletStateSyncManager.getInstance()
            .refreshByUser(prefs, database, texts, rescheduleRefreshJob)
    }

    fun gatherTokenInformation(tokenDbProvider: TokenDbProvider, apiService: ApiServiceManager) {

        // cancel former Jobs, if any
        tokenInformationJob?.cancel()

        // copy to an own list to prevent race conditions
        val tokensList = this.tokensList.toMutableList()

        // start gathering token information
        if (tokensList.isNotEmpty()) {
            tokenInformationJob = coroutineScope.launch {
                tokensList.forEach {
                    if (isActive) {
                        TokenInfoManager.getInstance()
                            .getTokenInformation(it.tokenId!!, tokenDbProvider, apiService)
                            ?.let {
                                synchronized(tokenInformation) {
                                    tokenInformation[it.tokenId] = it
                                    onNewTokenInfoGathered(it)
                                }
                            }
                    }
                }
            }
        }
    }

    fun getAddressLabel(texts: StringProvider) = walletAddress?.getAddressLabel(texts)
        ?: texts.getString(STRING_LABEL_ALL_ADDRESSES, wallet?.getNumOfAddresses() ?: 0)

    fun getErgoBalance(): ErgoAmount {
        val addressState = getAddressState()
        return ErgoAmount(addressState?.balance ?: wallet?.getBalanceForAllAddresses() ?: 0)
    }

    private fun getAddressState(): WalletState? {
        return walletAddress?.let { wallet?.getStateForAddress(it.publicAddress) }
    }

    fun getUnconfirmedErgoBalance() = ErgoAmount(
        getAddressState()?.unconfirmedBalance ?: wallet?.getUnconfirmedBalanceForAllAddresses() ?: 0
    )

    fun qrCodeScanned(
        qrCodeData: String,
        stringProvider: StringProvider,
        navigateToColdWalletSigning: ((signingData: String) -> Unit),
        navigateToErgoPaySigning: ((ergoPayRequest: String) -> Unit),
        navigateToSendFundsScreen: ((requestData: String) -> Unit),
        navigateToAuthentication: (String) -> Unit,
        showErrorMessage: ((errorMessage: String) -> Unit)
    ) {
        if (isColdSigningRequestChunk(qrCodeData)) {
            if (wallet?.walletConfig?.isReadOnly() == false)
                navigateToColdWalletSigning.invoke(qrCodeData)
            else
                showErrorMessage(stringProvider.getString(STRING_HINT_READONLY_SIGNING_REQUEST))
        } else if (isErgoPaySigningRequest(qrCodeData)) {
            navigateToErgoPaySigning.invoke(qrCodeData)
        } else if (isErgoAuthRequestUri(qrCodeData) || isErgoAuthRequestChunk(qrCodeData)) {
            navigateToAuthentication(qrCodeData)
        } else {
            val content = parsePaymentRequest(qrCodeData)
            content?.let {
                navigateToSendFundsScreen(qrCodeData)
            } ?: showErrorMessage(
                stringProvider.getString(
                    STRING_ERROR_QR_CODE_CONTENT_UNKNOWN
                )
            )
        }
    }

    suspend fun loadTransactionsToShow(transactionDbProvider: TransactionDbProvider): List<AddressTransactionWithTokens> {
        val addresses = getSelectedAddresses()?.map { it.publicAddress }

        return if (addresses?.size == 1) {
            transactionDbProvider.loadAddressTransactionsWithTokens(
                addresses.first(),
                maxTransactionsToShow,
                page = 0
            )
        } else {
            val returnedTransactions = mutableListOf<AddressTransactionWithTokens>()
            addresses?.forEach { address ->
                returnedTransactions.addAll(
                    transactionDbProvider.loadAddressTransactionsWithTokens(
                        address,
                        maxTransactionsToShow,
                        0
                    )
                )
            }
            returnedTransactions.sortedByDescending { it.addressTransaction.inclusionHeight }
                .take(maxTransactionsToShow)
        }
    }

    /**
     * returns true if the two lists given are not the same. use to determine if a rebinding of
     * transaction lists needs to be done
     */
    fun hasChangedNewTxList(
        newTransactionList: List<AddressTransactionWithTokens>,
        otherTxList: List<AddressTransactionWithTokens>?
    ) = otherTxList == null ||
            newTransactionList.size != otherTxList.size ||
            newTransactionList.isNotEmpty() && List(newTransactionList.size) {
        val newTx = newTransactionList[it].addressTransaction
        val shownTx = otherTxList[it].addressTransaction
        newTx.txId != shownTx.txId || newTx.state != shownTx.state
    }.reduceRight { a, b -> a || b }

    private fun getSelectedAddresses(): List<WalletAddress>? {
        return walletAddress?.let { listOf(it) } ?: wallet?.getSortedDerivedAddressesList()
    }

    abstract fun onDataChanged()

    abstract fun onNewTokenInfoGathered(tokenInformation: TokenInformation)

}