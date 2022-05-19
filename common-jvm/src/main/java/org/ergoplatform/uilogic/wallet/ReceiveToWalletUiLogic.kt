package org.ergoplatform.uilogic.wallet

import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.getExplorerPaymentRequestAddress
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.ErgoOrFiatAmount
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.ergoplatform.uilogic.STRING_LABEL_FIAT_AMOUNT
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.wallet.getDerivedAddressEntity

class ReceiveToWalletUiLogic {
    var derivationIdx: Int = 0
        set(value) {
            field = value
            refreshAddressInformation()
        }

    var wallet: Wallet? = null
        private set
    var address: WalletAddress? = null
        private set
    val amountToReceive = ErgoOrFiatAmount()

    suspend fun loadWallet(walletId: Int, walletDbProvider: WalletDbProvider) {
        wallet = walletDbProvider.loadWalletWithStateById(walletId)
        refreshAddressInformation()
    }

    private fun refreshAddressInformation() {
        address = wallet?.getDerivedAddressEntity(derivationIdx)
            ?: wallet?.getDerivedAddressEntity(0)
    }

    fun getTextToShare(purpose: String): String? {
        return address?.publicAddress?.let {
            getExplorerPaymentRequestAddress(
                it,
                amountToReceive.ergAmount,
                purpose
            )
        }
    }

    fun getOtherCurrencyLabel(textProvider: StringProvider): String? {
        val nodeConnector = WalletStateSyncManager.getInstance()
        return if (nodeConnector.fiatCurrency.isNotEmpty()) {
            if (!amountToReceive.inputIsFiat) {
                textProvider.getString(
                    STRING_LABEL_FIAT_AMOUNT,
                    formatFiatToString(
                        amountToReceive.ergAmount.toDouble() * nodeConnector.fiatValue.value.toDouble(),
                        nodeConnector.fiatCurrency, textProvider
                    )
                )
            } else {
                textProvider.getString(
                    STRING_LABEL_FIAT_AMOUNT,
                    textProvider.getString(
                        STRING_LABEL_ERG_AMOUNT,
                        amountToReceive.ergAmount.toStringRoundToDecimals()
                    )
                )
            }
        } else
            null
    }
}