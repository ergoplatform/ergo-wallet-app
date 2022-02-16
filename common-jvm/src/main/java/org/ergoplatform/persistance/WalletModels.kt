package org.ergoplatform.persistance

const val ENC_TYPE_PASSWORD = 1

/**
 * Indicates some kind of device-bound encryption: Device encryption on Android, Keychain on iOS
 */
const val ENC_TYPE_DEVICE = 2

data class WalletConfig(
    val id: Int,
    val displayName: String?,
    val firstAddress: String?,
    val encryptionType: Int?,
    val secretStorage: ByteArray?,
    val unfoldTokens: Boolean = false,
    val extendedPublicKey: String?,
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
    val label: String?,
)

data class Wallet(
    val walletConfig: WalletConfig,
    val state: List<WalletState>,
    val tokens: List<WalletToken>,
    val addresses: List<WalletAddress>
)
