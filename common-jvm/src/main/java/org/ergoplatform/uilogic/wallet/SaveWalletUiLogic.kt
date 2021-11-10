package org.ergoplatform.uilogic.wallet

import org.ergoplatform.NodeConnector
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_DEFAULT
import org.ergoplatform.uilogic.StringProvider

class SaveWalletUiLogic(val mnemonic: SecretString) {

    val publicAddress get() = getPublicErgoAddressFromMnemonic(mnemonic)

    /**
     * Saves the wallet data to DB
     */
    suspend fun suspendSaveToDb(
        walletDbProvider: WalletDbProvider,
        strings: StringProvider,
        encType: Int,
        secretStorage: ByteArray?
    ) {
        val publicAddress = this.publicAddress

        // check if the wallet already exists
        val existingWallet = walletDbProvider.loadWalletByFirstAddress(publicAddress)

        if (existingWallet != null) {
            // update encType and secret storage
            val walletConfig = WalletConfig(
                existingWallet.id,
                existingWallet.displayName,
                existingWallet.firstAddress,
                encType,
                secretStorage
            )
            walletDbProvider.updateWalletConfig(walletConfig)
        } else {
            val walletConfig =
                WalletConfig(
                    0,
                    strings.getString(STRING_LABEL_WALLET_DEFAULT),
                    publicAddress,
                    encType,
                    secretStorage
                )
            walletDbProvider.insertWalletConfig(walletConfig)
            NodeConnector.getInstance().invalidateCache()
        }
    }

    fun isPasswordWeak(password: String?): Boolean {
        return password == null || password.length < 8
    }
}