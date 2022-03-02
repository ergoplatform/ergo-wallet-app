package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ErgoApiService
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.*
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.transactions.isColdSigningRequestChunk
import org.ergoplatform.transactions.isErgoPaySigningRequest
import org.ergoplatform.uilogic.STRING_ERROR_QR_CODE_CONTENT_UNKNOWN
import org.ergoplatform.uilogic.STRING_LABEL_ALL_ADDRESSES
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.wallet.*
import org.ergoplatform.wallet.addresses.getAddressLabel

abstract class WalletDetailsUiLogic {
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

    abstract val coroutineScope: CoroutineScope

    fun setUpWalletStateFlowCollector(database: IAppDatabase, walletId: Int) {
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

    fun newAddressIdxChosen(newAddressIdx: Int?) {
        if (newAddressIdx != addressIdx) {
            addressIdx = newAddressIdx
            refreshAddress()
        }
    }

    private fun refreshAddress() {
        walletAddress = addressIdx?.let { wallet?.getDerivedAddressEntity(it) }
        tokensList = (walletAddress?.let { wallet?.getTokensForAddress(it.publicAddress) }
            ?: wallet?.getTokensForAllAddresses() ?: emptyList()).sortedBy { it.name?.lowercase() }

        onDataChanged()
    }

    fun gatherTokenInformation(tokenDbProvider: TokenDbProvider, apiService: ErgoApiService) {

        // cancel former Jobs, if any
        tokenInformationJob?.cancel()

        // copy to an own list to prevent race conditions
        val tokensList = this.tokensList.toMutableList()

        // start gathering token information
        if (tokensList.isNotEmpty()) {
            tokenInformationJob = coroutineScope.launch {
                tokensList.forEach {
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
        showErrorMessage: ((errorMessage: String) -> Unit)
    ) {
        if (wallet?.walletConfig?.secretStorage != null && isColdSigningRequestChunk(qrCodeData)) {
            navigateToColdWalletSigning.invoke(qrCodeData)
        } else if (isErgoPaySigningRequest(qrCodeData)) {
            navigateToErgoPaySigning.invoke(qrCodeData)
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

    abstract fun onDataChanged()

    abstract fun onNewTokenInfoGathered(tokenInformation: TokenInformation)

}