package org.ergoplatform.persistance

import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.appkit.ErgoValue
import java.math.BigDecimal

/**
 * Persistence entity class representing a token price
 */
data class TokenPrice(
    val tokenId: String,
    val displayName: String?,
    val priceSource: String,
    val ergValue: BigDecimal
)

/**
 * Persistence entity class representing token information.
 * Compared to [WalletToken] (which represents a token balance in a user's wallet), this references
 * general, wallet-independent information regarding the token
 */
data class TokenInformation(
    // immutable over token lifetime
    val tokenId: String,
    val issuingBoxId: String,
    val mintingTxId: String,
    val displayName: String,
    val description: String,
    val decimals: Int,
    val fullSupply: Long,
    val reg7hex: String?,
    val reg8hex: String?,
    val reg9hex: String?,

    // these need to be updatable
    val genuineFlag: Int = GENUINE_UNKNOWN,
    val issuerLink: String? = null,
    val thumbnailBytes: ByteArray? = null,
    val thumbnailType: Int = THUMBNAIL_TYPE_NONE,
    val updatedMs: Long = 0
) {
    constructor(
        tokenInformation: TokenInformation,
        genuineFlag: Int = GENUINE_UNKNOWN,
        issuerLink: String? = null,
        thumbnailBytes: ByteArray? = null,
        thumbnailType: Int = THUMBNAIL_TYPE_NONE,
        updatedMs: Long = 0
    ) : this(
        tokenInformation.tokenId,
        tokenInformation.issuingBoxId, tokenInformation.mintingTxId,
        tokenInformation.displayName, tokenInformation.description, tokenInformation.decimals,
        tokenInformation.fullSupply, tokenInformation.reg7hex, tokenInformation.reg8hex,
        tokenInformation.reg9hex,
        genuineFlag, issuerLink, thumbnailBytes, thumbnailType, updatedMs
    )

    /**
     * constructs [Eip4Token] class from persisted data entity
     */
    fun toEip4Token(): Eip4Token = Eip4Token(
        tokenId,
        fullSupply,
        displayName,
        description,
        decimals,
        reg7hex?.let { ErgoValue.fromHex(it) },
        reg8hex?.let { ErgoValue.fromHex(it) },
        reg9hex?.let { ErgoValue.fromHex(it) }
    )
}

/**
 * [TokenInformation.thumbnailType] - no thumbnail shown
 */
const val THUMBNAIL_TYPE_NONE = 0
/**
 * [TokenInformation.thumbnailType] - image NFT thumbnail shown
 */
const val THUMBNAIL_TYPE_NFT_IMG = 10
/**
 * [TokenInformation.thumbnailType] - audio NFT thumbnail shown
 */
const val THUMBNAIL_TYPE_NFT_AUDIO = 11
/**
 * [TokenInformation.thumbnailType] - video NFT thumbnail shown
 */
const val THUMBNAIL_TYPE_NFT_VID = 12

/**
 * [TokenInformation.thumbnailType] - for future use
 */
const val THUMBNAIL_TYPE_BYTES_PNG = 20


/**
 * [TokenInformation.genuineFlag] - genuine state undefined or not known
 */
const val GENUINE_UNKNOWN = 0
/**
 * [TokenInformation.genuineFlag] - genuine state verified (EIP-21)
 */
const val GENUINE_VERIFIED = 1
/**
 * [TokenInformation.genuineFlag] - genuine state suspicious (EIP-21)
 */
const val GENUINE_SUSPICIOUS = 2
/**
 * [TokenInformation.genuineFlag] - genuine state blocked (EIP-21)
 */
const val GENUINE_BLOCKED = 3
