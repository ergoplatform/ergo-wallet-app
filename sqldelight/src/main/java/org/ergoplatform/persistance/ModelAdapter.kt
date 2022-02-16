package org.ergoplatform.persistance

fun Wallet_configs.toModel(): WalletConfig {
    return WalletConfig(
        id.toInt(),
        display_name,
        public_address,
        enc_type,
        secret_storage,
        unfold_tokens,
        xpubkey
    )
}

fun WalletConfig.toDbEntity(): Wallet_configs {
    return Wallet_configs(
        id.toLong(),
        displayName,
        firstAddress,
        encryptionType,
        secretStorage,
        unfoldTokens,
        extendedPublicKey
    )
}

fun WalletState.toDbEntity(): Wallet_states {
    return Wallet_states(publicAddress, walletFirstAddress, balance, unconfirmedBalance)
}

fun Wallet_states.toModel(): WalletState {
    return WalletState(public_address, wallet_first_address, balance, unconfirmed_balance)
}

fun Wallet_addresses.toModel(): WalletAddress {
    return WalletAddress(id, wallet_first_address, index, public_address, label)
}

fun Wallet_tokens.toModel(): WalletToken {
    return WalletToken(
        id,
        public_address,
        wallet_first_address,
        token_id,
        amount,
        decimals ?: 0,
        name
    )
}