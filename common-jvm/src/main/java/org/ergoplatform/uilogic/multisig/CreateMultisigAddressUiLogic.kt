package org.ergoplatform.uilogic.multisig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.MultisigAddress
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.WALLET_TYPE_MULTISIG
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.STRING_ERROR_ADDRESS_ALREADY_ADDED
import org.ergoplatform.uilogic.STRING_ERROR_INVALID_READONLY_INPUT
import org.ergoplatform.uilogic.STRING_LABEL_MULTISIG_WALLET
import org.ergoplatform.uilogic.StringProvider

class CreateMultisigAddressUiLogic {
    val participants = ArrayList<Address>()

    fun addParticipantAddress(qrCodeOrAddress: String, texts: StringProvider) {
        // if we have a payment request, we extract the address
        val content = parsePaymentRequest(qrCodeOrAddress)
        val addressToAdd = content?.address ?: qrCodeOrAddress

        if (!isValidErgoAddress(addressToAdd))
            throw IllegalArgumentException(texts.getString(STRING_ERROR_INVALID_READONLY_INPUT))

        val ergoAddress = Address.create(addressToAdd)

        if (!ergoAddress.isP2PK)
            throw IllegalArgumentException(texts.getString(STRING_ERROR_INVALID_READONLY_INPUT))

        if (participants.none { it == ergoAddress })
            participants.add(ergoAddress)
    }

    fun removeParticipantAddress(address: String) {
        participants.removeAll { it == Address.create(address) }
    }

    suspend fun addWalletToDb(
        signersNeeded: Int,
        walletDbProvider: WalletDbProvider,
        stringProvider: StringProvider,
        displayName: String?
    ) {
        // calc address
        val address = MultisigAddress.buildFromParticipants(signersNeeded, participants).address.toString()

        val existingWallet = walletDbProvider.loadWalletByFirstAddress(address)

        existingWallet?.let {
            throw IllegalArgumentException(
                stringProvider.getString(
                    STRING_ERROR_ADDRESS_ALREADY_ADDED,
                    existingWallet.displayName ?: ""
                )
            )
        }

        GlobalScope.launch(Dispatchers.IO) {
            val walletConfig = WalletConfig(
                0,
                if (!displayName.isNullOrBlank()) displayName
                else stringProvider.getString(STRING_LABEL_MULTISIG_WALLET),
                address,
                0,
                null,
                walletType = WALLET_TYPE_MULTISIG,
                extendedPublicKey = null
            )

            walletDbProvider.insertWalletConfig(walletConfig)
            WalletStateSyncManager.getInstance().invalidateCache()
        }
    }
}