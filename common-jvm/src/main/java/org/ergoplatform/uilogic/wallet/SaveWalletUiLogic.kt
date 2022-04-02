package org.ergoplatform.uilogic.wallet

import org.ergoplatform.SigningSecrets
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_DEFAULT
import org.ergoplatform.uilogic.StringProvider

class SaveWalletUiLogic(val mnemonic: SecretString, fromRestore: Boolean) {

    // Constructor

    private var useDeprecatedDerivation: Boolean = false

    var publicAddress: String = getPublicErgoAddressFromMnemonic(signingSecrets)
        private set

    val hasAlternativeAddress = fromRestore &&
            (!publicAddress.equals(
                getPublicErgoAddressFromMnemonic(
                    SigningSecrets(mnemonic, true)
                )
            ))

    // methods

    val signingSecrets get() = SigningSecrets(mnemonic, useDeprecatedDerivation)

    fun switchAddress() {
        if (hasAlternativeAddress) {
            useDeprecatedDerivation = !useDeprecatedDerivation
            publicAddress = getPublicErgoAddressFromMnemonic(signingSecrets)
        }
    }

    suspend fun getSuggestedDisplayName(
        walletDbProvider: WalletDbProvider,
        strings: StringProvider
    ) = getExistingWallet(walletDbProvider)?.displayName
        ?: strings.getString(STRING_LABEL_WALLET_DEFAULT)

    private suspend fun getExistingWallet(walletDbProvider: WalletDbProvider) =
        walletDbProvider.loadWalletByFirstAddress(publicAddress)

    /**
     * show display name input text field only when there are already wallets set up
     */
    fun showSuggestedDisplayName(walletDbProvider: WalletDbProvider) =
        walletDbProvider.getAllWalletConfigsSynchronous().isNotEmpty()

    /**
     * Saves the wallet data to DB
     */
    suspend fun suspendSaveToDb(
        walletDbProvider: WalletDbProvider,
        displayName: String,
        encType: Int,
        secretStorage: ByteArray?
    ) {
        val publicAddress = this.publicAddress

        // check if the wallet already exists
        val existingWallet = getExistingWallet(walletDbProvider)

        if (existingWallet != null) {
            // update encType and secret storage, removes existing xpubkey
            val walletConfig = WalletConfig(
                existingWallet.id,
                displayName,
                existingWallet.firstAddress,
                encType,
                secretStorage,
                extendedPublicKey = null
            )
            walletDbProvider.updateWalletConfig(walletConfig)
        } else {
            val walletConfig =
                WalletConfig(
                    0,
                    displayName,
                    publicAddress,
                    encType,
                    secretStorage,
                    extendedPublicKey = null
                )
            walletDbProvider.insertWalletConfig(walletConfig)
            WalletStateSyncManager.getInstance().invalidateCache()
        }
    }

    fun isPasswordWeak(password: String?): Boolean {
        return password == null || password.length < 8
    }
}