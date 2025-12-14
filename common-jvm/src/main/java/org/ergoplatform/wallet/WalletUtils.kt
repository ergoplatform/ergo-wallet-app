package org.ergoplatform.wallet

import org.ergoplatform.persistance.*
import org.ergoplatform.wallet.addresses.ensureWalletAddressListHasFirstAddress

/**
 * sums up all balances of derived address
 */
fun Wallet.getBalanceForAllAddresses(): Long {
    return state.map { it.balance ?: 0 }.sum()
}

/**
 * sums up all unconfirmed balances of derived address
 */
fun Wallet.getUnconfirmedBalanceForAllAddresses(): Long {
    return state.map { it.unconfirmedBalance ?: 0 }.sum()
}

fun Wallet.getTokensForAllAddresses(): List<WalletToken> {
    // combine tokens with same id but different list entries
    val hashmap = HashMap<String, WalletToken>()

    tokens.forEach {
        hashmap.put(it.tokenId!!, hashmap.get(it.tokenId)?.let { tokenInMap ->
            WalletToken(
                0, "", it.walletFirstAddress, it.tokenId,
                (it.amount ?: 0) + (tokenInMap.amount ?: 0), it.decimals, it.name
            )
        } ?: it)
    }

    val combinedTokens = hashmap.values
    return if (combinedTokens.size < tokens.size) combinedTokens.toList() else tokens
}

fun Wallet.getTokensForAddress(address: String): List<WalletToken> {
    return tokens.filter { it.publicAddress.equals(address) }
}

/**
 * @return derived address string with given index
 */
fun Wallet.getDerivedAddress(derivationIdx: Int): String? {
    // edge case: index 0 is not (always) part of addresses table
    if (derivationIdx == 0) {
        return walletConfig.firstAddress
    } else {
        return addresses.firstOrNull { it.derivationIndex == derivationIdx }?.publicAddress
    }
}

/**
 * @return derived address entity with given derivation index
 */
fun Wallet.getDerivedAddressEntity(derivationIdx: Int): WalletAddress? {
    val allAddresses =
        ensureWalletAddressListHasFirstAddress(
            addresses,
            walletConfig.firstAddress!!
        )
    return allAddresses.firstOrNull { it.derivationIndex == derivationIdx }
}

/**
 * @return derived addresses list, making sure that 0 address is included and list is sorted by idx
 */
fun Wallet.getSortedDerivedAddressesList(): List<WalletAddress> {
    val retList = ensureWalletAddressListHasFirstAddress(
        addresses,
        walletConfig.firstAddress!!
    )
    return retList.sortedBy { it.derivationIndex }
}

fun Wallet.getNumOfAddresses(): Int {
    // edge case: index 0 is not (always) part of addresses table
    return addresses.filter { it.derivationIndex != 0 }.size + 1
}

fun Wallet.getStateForAddress(address: String): WalletState? {
    return state.filter { it.publicAddress.equals(address) }.firstOrNull()
}

fun WalletConfig.isReadOnly(): Boolean = secretStorage == null

fun WalletConfig.isMultisig(): Boolean = walletType == WALLET_TYPE_MULTISIG

fun WalletConfig.getWalletTypeName(): String = when (walletType) {
    WALLET_TYPE_P2PK -> "Standard"
    WALLET_TYPE_READ_ONLY -> "Read-Only"
    WALLET_TYPE_MULTISIG -> "Multisig"
    else -> "Unknown"
}

fun Wallet.isReadOnly(): Boolean = walletConfig.isReadOnly()

fun Wallet.isMultisig(): Boolean = walletConfig.isMultisig()

/**
 * Returns the oldest (minimum) lastSyncTime from all wallet addresses
 * Used to display when the wallet was last fully synced
 * @return timestamp in milliseconds, or null if no addresses have been synced
 */
fun Wallet.getOldestSyncTime(): Long? {
    return state.mapNotNull { it.lastSyncTime }.minOrNull()
}

/**
 * Returns the lastSyncTime for a specific address
 * @param address the public address to check
 * @return timestamp in milliseconds, or null if address hasn't been synced
 */
fun Wallet.getSyncTimeForAddress(address: String): Long? {
    return getStateForAddress(address)?.lastSyncTime
}

fun List<Wallet>.sortedByDisplayName(): List<Wallet> = sortedBy { it.walletConfig.displayName?.lowercase() }