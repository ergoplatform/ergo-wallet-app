package org.ergoplatform.uilogic.multisig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.MultisigAddress
import org.ergoplatform.getErgoNetworkType
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.WALLET_TYPE_MULTISIG
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.*

class CreateMultisigAddressUiLogic {
    val participants = ArrayList<Address>()
    val minSignersNeeded = 2

    fun defaultWalletName(texts: StringProvider) = texts.getString(STRING_LABEL_MULTISIG_WALLET)

    fun addParticipantAddress(addressToAdd: String, texts: StringProvider) {
        if (!isValidErgoAddress(addressToAdd))
            throw IllegalArgumentException(texts.getString(STRING_ERROR_RECEIVER_ADDRESS))

        val ergoAddress = Address.create(addressToAdd)

        if (!ergoAddress.isP2PK)
            throw IllegalArgumentException(texts.getString(STRING_ERROR_RECEIVER_ADDRESS))

        if (participants.none { it == ergoAddress })
            participants.add(ergoAddress)
    }

    fun removeParticipantAddress(address: Address) {
        participants.removeAll { it == address }
    }

    suspend fun addWalletToDb(
        signersNeeded: Int,
        walletDbProvider: WalletDbProvider,
        stringProvider: StringProvider,
        displayName: String?
    ) {
        if (signersNeeded <= 0 || signersNeeded > participants.size)
            throw IllegalArgumentException(stringProvider.getString(STRING_ERROR_NUM_SIGNERS))

        // calc address
        val address =
            MultisigAddress.buildFromParticipants(
                signersNeeded,
                participants,
                getErgoNetworkType(),
            ).address.toString()

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

    fun getInputFromQrCode(qrCode: String): String {
        // if we have a payment request, we extract the address
        val content = parsePaymentRequest(qrCode)
        return content?.address ?: qrCode
    }
}