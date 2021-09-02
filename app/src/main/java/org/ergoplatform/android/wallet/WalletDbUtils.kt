package org.ergoplatform.android.wallet

/**
 * sums up all balances of derived address
 */
fun WalletDbEntity.getBalanceForAllAddresses(): Long {
    return state.map { it.balance ?: 0 }.sum()
}

/**
 * sums up all unconfirmed balances of derived address
 */
fun WalletDbEntity.getUnconfirmedBalanceForAllAddresses(): Long {
    return state.map { it.unconfirmedBalance ?: 0 }.sum()
}

fun WalletDbEntity.getTokensForAllAddresses(): List<WalletTokenDbEntity> {
    // TODO combine tokens with same id but different list entries
    return tokens
}

fun WalletDbEntity.getTokensForAddress(address: String): List<WalletTokenDbEntity> {
    return tokens.filter { it.publicAddress.equals(address) }
}

/**
 * @return derived address with given index
 */
fun WalletDbEntity.getDerivedAddress(idx: Int): String? {
    // edge case: index 0 is not (always) part of addresses table
    if (idx == 0) {
        return walletConfig.firstAddress
    } else {
        return addresses.filter { it.derivationIndex == idx }.firstOrNull()?.publicAddress
    }

}

fun WalletDbEntity.getNumOfAddresses(): Int {
    // edge case: index 0 is not (always) part of addresses table
    return addresses.filter { it.derivationIndex != 0 }.size + 1
}

fun WalletDbEntity.getStateForAddress(address: String): WalletStateDbEntity? {
    return state.filter { it.publicAddress.equals(address) }.firstOrNull()
}