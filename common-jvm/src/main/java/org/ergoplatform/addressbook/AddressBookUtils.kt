package org.ergoplatform.addressbook

import org.ergoplatform.getFeeAddressAsString
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.uilogic.STRING_LABEL_ADDRESS_TX_FEE
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_MAIN_ADDRESS
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.wallet.addresses.getAddressLabel

fun List<AddressBookEntry>.sortedByLabel() = sortedBy { it.label.lowercase() }

/**
 * @return address label using address book and own addresses list
 */
suspend fun getAddressLabelFromDatabase(
    database: IAppDatabase,
    address: String,
    stringProvider: StringProvider
): String? {
    if (address == getFeeAddressAsString())
        return stringProvider.getString(STRING_LABEL_ADDRESS_TX_FEE)

    val addrBookEntry = database.addressBookDbProvider.findAddressEntry(address)

    if (addrBookEntry != null)
        return addrBookEntry.label

    val walletAddress = database.walletDbProvider.loadWalletAddress(address)
    val walletConfig = database.walletDbProvider.loadWalletByFirstAddress(
        walletAddress?.walletFirstAddress ?: address
    )

    if (walletAddress != null)
        return "${walletConfig?.displayName}: ${walletAddress.getAddressLabel(stringProvider)}"

    return walletConfig?.let {
        return "${walletConfig.displayName}: ${
            stringProvider.getString(STRING_LABEL_WALLET_MAIN_ADDRESS)
        }"
    }
}