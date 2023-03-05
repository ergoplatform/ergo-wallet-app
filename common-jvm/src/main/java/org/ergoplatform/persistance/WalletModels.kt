package org.ergoplatform.persistance

const val ENC_TYPE_PASSWORD = 1

/**
 * Indicates some kind of device-bound encryption: Device encryption on Android, Keychain on iOS
 */
const val ENC_TYPE_DEVICE = 2

/**
 * a "normal" wallet: p2pk and signing if secretStorage is filled, read-only if not
 */
const val WALLET_TYPE_NORMAL = 0

/**
 * a multisig wallet. will never have secretStorage filled and won't have derived addresses
 */
const val WALLET_TYPE_MULTISIG = 10

data class WalletConfig(
    val id: Int,
    val displayName: String?,
    val firstAddress: String?,
    val encryptionType: Int?,
    val secretStorage: ByteArray?,
    val unfoldTokens: Boolean = false,
    val extendedPublicKey: String?,
    val walletType: Int,
)

data class WalletState(
    val publicAddress: String,
    val walletFirstAddress: String,
    val balance: Long?,
    val unconfirmedBalance: Long?
)

data class WalletToken(
    val id: Long,
    val publicAddress: String,
    val walletFirstAddress: String,
    val tokenId: String?,
    val amount: Long?,
    val decimals: Int,
    val name: String?,
)

data class WalletAddress(
    val id: Long,
    val walletFirstAddress: String,
    val derivationIndex: Int,
    val publicAddress: String,
    override val label: String?,
) : IAddressWithLabel {
    override val address: String get() = publicAddress
}

data class Wallet(
    val walletConfig: WalletConfig,
    val state: List<WalletState>,
    val tokens: List<WalletToken>,
    val addresses: List<WalletAddress>
)
