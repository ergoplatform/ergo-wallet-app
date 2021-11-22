package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.STRING_ERROR_RECEIVER_ADDRESS
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_DEFAULT
import org.ergoplatform.uilogic.StringProvider

abstract class AddReadOnlyWalletUiLogic(val stringProvider: StringProvider) {
    fun addWalletToDb(walletAddress: String, walletDbProvider: WalletDbProvider): Boolean {
        if (!isValidErgoAddress(walletAddress)) {
            setErrorMessage(stringProvider.getString(STRING_ERROR_RECEIVER_ADDRESS))
            return false
        } else {
            val walletConfig =
                WalletConfig(
                    0,
                    stringProvider.getString(STRING_LABEL_WALLET_DEFAULT),
                    walletAddress,
                    0,
                    null
                )

            GlobalScope.launch(Dispatchers.IO) {
                // make sure not to use dialog context within this block
                val existingWallet = walletDbProvider.loadWalletByFirstAddress(walletAddress)
                if (existingWallet == null) {
                    walletDbProvider.insertWalletConfig(walletConfig)
                    NodeConnector.getInstance().invalidateCache()
                }
            }
            return true
        }
    }

    abstract fun setErrorMessage(message: String)
}