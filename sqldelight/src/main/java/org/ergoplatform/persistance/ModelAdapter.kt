package org.ergoplatform.persistance

import java.math.BigDecimal

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

fun Token_price.toModel(): TokenPrice {
    return TokenPrice(tokenId, display_name, source, BigDecimal(erg_value))
}

fun TokenPrice.toDbEntity(): Token_price {
    return Token_price(tokenId, displayName, priceSource, ergValue.toString())
}

fun Token_info.toModel(): TokenInformation {
    return TokenInformation(
        tokenId,
        issuing_box,
        minting_tx,
        display_name,
        description,
        decimals.toInt(),
        full_supply,
        reg7,
        reg8,
        reg9,
        genuine_flag.toInt(),
        issuer_link,
        thumbnail_bytes,
        thunbnail_type.toInt(),
        updated_ms
    )
}

fun TokenInformation.toDbEntity(): Token_info {
    return Token_info(
        tokenId,
        issuingBoxId,
        mintingTxId,
        displayName,
        description,
        decimals.toLong(),
        fullSupply,
        reg7hex,
        reg8hex,
        reg9hex,
        genuineFlag.toLong(),
        issuerLink,
        thumbnailBytes,
        thumbnailType.toLong(),
        updatedMs
    )
}