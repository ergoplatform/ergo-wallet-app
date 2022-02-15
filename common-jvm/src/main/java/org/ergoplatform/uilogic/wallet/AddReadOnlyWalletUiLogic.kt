package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.Bip32Serialization
import org.ergoplatform.getErgoNetworkType
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.*

abstract class AddReadOnlyWalletUiLogic {
    suspend fun addWalletToDb(
        userInput: String,
        walletDbProvider: WalletDbProvider,
        stringProvider: StringProvider,
        displayName: String?
    ): Boolean {
        val xpubkey = try {
            Bip32Serialization.parseExtendedPublicKeyFromHex(userInput, getErgoNetworkType())
        } catch (t: Throwable) {
            null
        }

        val walletAddress = xpubkey?.let {
            Address.createEip3Address(0, getErgoNetworkType(), xpubkey).toString()
        } ?: userInput

        // check for valid address
        if (!isValidErgoAddress(walletAddress)) {
            setErrorMessage(stringProvider.getString(STRING_ERROR_INVALID_READONLY_INPUT))
            return false
        }

        // check if this address already is added to an existing wallet
        // or is a main wallet
        val derivedAddress = walletDbProvider.loadWalletAddress(walletAddress)
        val existingWallet = derivedAddress?.let {
            walletDbProvider.loadWalletByFirstAddress(derivedAddress.walletFirstAddress)
        } ?: walletDbProvider.loadWalletByFirstAddress(walletAddress)
        existingWallet?.let {
            setErrorMessage(
                stringProvider.getString(
                    STRING_ERROR_ADDRESS_ALREADY_ADDED,
                    existingWallet.displayName ?: ""
                )
            )
            return false
        }

        val walletConfig = WalletConfig(
            0,
            if (!displayName.isNullOrBlank()) displayName
            else stringProvider.getString(STRING_LABEL_READONLY_WALLET_DEFAULT),
            walletAddress,
            0,
            null,
            extendedPublicKey = xpubkey?.let { userInput }
        )

        GlobalScope.launch(Dispatchers.IO) {
            walletDbProvider.insertWalletConfig(walletConfig)
            NodeConnector.getInstance().invalidateCache()
        }
        return true
    }

    abstract fun setErrorMessage(message: String)

    fun getInputFromQrCode(qrCodeData: String): String {
        // if we have a payment request, we extract the address
        val content = parsePaymentRequest(qrCodeData)
        return content?.address ?: qrCodeData
    }
}