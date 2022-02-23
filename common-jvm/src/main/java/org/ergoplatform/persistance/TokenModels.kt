package org.ergoplatform.persistance

import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.appkit.ErgoValue
import java.math.BigDecimal

data class TokenPrice(
    val tokenId: String,
    val displayName: String?,
    val priceSource: String,
    val ergValue: BigDecimal
)

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

const val THUMBNAIL_TYPE_NONE = 0
const val THUMBNAIL_TYPE_NFT_IMG = 10
const val THUMBNAIL_TYPE_NFT_AUDIO = 11
const val THUMBNAIL_TYPE_NFT_VID = 12
const val THUMBNAIL_TYPE_BYTES_PNG = 20

const val GENUINE_UNKNOWN = 0
const val GENUINE_VERIFIED = 1
const val GENUINE_SUSPICIOUS = 2
const val GENUINE_BLOCKED = 3
