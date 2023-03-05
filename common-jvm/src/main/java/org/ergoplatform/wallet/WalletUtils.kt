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
    return tokens.filter { it.publicAddress == address }
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
    return state.firstOrNull { it.publicAddress == address }
}

fun WalletConfig.isReadOnly(): Boolean = secretStorage == null
fun Wallet.isReadOnly(): Boolean = walletConfig.isReadOnly()

fun Wallet.isMultisig(): Boolean = walletConfig.isMultisig()
fun WalletConfig.isMultisig(): Boolean = walletType == WALLET_TYPE_MULTISIG

fun List<Wallet>.sortedByDisplayName(): List<Wallet> =
    sortedBy { it.walletConfig.displayName?.lowercase() }