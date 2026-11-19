package org.ergoplatform.wallet.addresses

import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_ADDRESS_DERIVED
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_MAIN_ADDRESS
import org.ergoplatform.uilogic.StringProvider

fun WalletAddress.getAddressLabel(stringProvider: StringProvider): String {
    return (label ?: (if (isDerivedAddress()) stringProvider.getString(
        STRING_LABEL_WALLET_ADDRESS_DERIVED,
        derivationIndex.toString()
    ) else stringProvider.getString(STRING_LABEL_WALLET_MAIN_ADDRESS)))
}

fun WalletAddress.isDerivedAddress(): Boolean {
    return derivationIndex > 0
}

fun ensureWalletAddressListHasFirstAddress(
    addresses: List<WalletAddress>,
    firstAddress: String
): List<WalletAddress> {
    val hasFirst = addresses.filter { it.derivationIndex == 0 }.isNotEmpty()
    val retList = addresses.toMutableList()

    if (!hasFirst) {
        retList.add(
            0,
            WalletAddress(
                0,
                firstAddress,
                0,
                firstAddress,
                null
            )
        )
    }
    return retList
}

/**
 * @return pair walletConfig id, derivation index of p2pkAddress, or null if not found
 */
suspend fun findWalletConfigAndAddressIdx(p2pkAddress: String, database: WalletDbProvider): Pair<Int, Int>? {
    val walletDerivedAddress = database.loadWalletAddress(p2pkAddress)
    val walletConfig = database.loadWalletByFirstAddress(
        walletDerivedAddress?.walletFirstAddress ?: p2pkAddress
    )

    return walletConfig?.let {
        walletConfig.id to (walletDerivedAddress?.derivationIndex ?: 0)
    }
}