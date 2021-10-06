package org.ergoplatform.persistance

fun Wallet_configs.toModel(): WalletConfig {
    return WalletConfig(id, display_name, public_address, enc_type, secret_storage, unfold_tokens)
}

fun WalletConfig.toDbEntity(): Wallet_configs {
    return Wallet_configs(id, displayName, firstAddress, encryptionType, secretStorage, unfoldTokens)
}