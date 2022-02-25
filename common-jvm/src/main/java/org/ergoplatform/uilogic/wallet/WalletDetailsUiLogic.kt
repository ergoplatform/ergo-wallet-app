package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.flow.collect
import org.ergoplatform.ErgoAmount
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.*
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

    suspend fun setUpWalletStateFlowCollector(walletDbProvider: WalletDbProvider, walletId: Int) {
        walletDbProvider.walletWithStateByIdAsFlow(walletId).collect {
            // called every time something changes in the DB
            onWalletStateChanged(it)
        }
    }

    /**
     * this needs to be public and callable from outside because on some platforms
     * walletWithStateByIdAsFlow does not cover all state changes, but only config changes
     */
    fun onWalletStateChanged(it: Wallet?) {
        wallet = it

        // no address set (yet) and there is only a single address available, fix it to this one
        if (addressIdx == null && wallet?.getNumOfAddresses() == 1) {
            addressIdx = 0
        }
        // make sure to post to observer the first time or on DB change
        refreshAddress()
    }

    fun setAddressIdx(newAddressIdx: Int?) {
        addressIdx = newAddressIdx
        refreshAddress()
    }

    private fun refreshAddress() {
        walletAddress = addressIdx?.let { wallet?.getDerivedAddressEntity(it) }
        onDataChanged()
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

    fun getTokensList(): List<WalletToken> {
        return (walletAddress?.let { wallet?.getTokensForAddress(it.publicAddress) }
            ?: wallet?.getTokensForAllAddresses() ?: emptyList()).sortedBy { it.name?.lowercase() }
    }

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
}