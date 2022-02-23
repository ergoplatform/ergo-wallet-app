package org.ergoplatform.android.tokens

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ergoplatform.persistance.GENUINE_UNKNOWN
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.TokenPrice
import java.math.BigDecimal

@Entity(tableName = "token_price")
data class TokenPriceDbEntity(
    @PrimaryKey val tokenId: String,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "source") val priceSource: String,
    @ColumnInfo(name = "erg_value") val ergValue: String
) {
    fun toModel(): TokenPrice {
        return TokenPrice(
            tokenId,
            displayName,
            priceSource,
            BigDecimal(ergValue)
        )
    }
}

fun TokenPrice.toDbEntity(): TokenPriceDbEntity {
    return TokenPriceDbEntity(
        tokenId,
        displayName,
        priceSource,
        ergValue.toString()
    )
}

@Entity(tableName = "token_info")
data class TokenInformationDbEntity(
    @PrimaryKey val tokenId: String,
    @ColumnInfo(name = "issuing_box") val issuingBoxId: String,
    @ColumnInfo(name = "minting_tx") val mintingTxId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "decimals") val decimals: Int,
    @ColumnInfo(name = "full_supply") val fullSupply: Long,
    @ColumnInfo(name = "reg7") val reg7hex: String?,
    @ColumnInfo(name = "reg8") val reg8hex: String?,
    @ColumnInfo(name = "reg9") val reg9hex: String?,
    @ColumnInfo(name = "genuine_flag") val genuineFlag: Int = GENUINE_UNKNOWN,
    @ColumnInfo(name = "issuer_link") val issuerLink: String? = null,
    @ColumnInfo(name = "thumbnail_bytes") val thumbnailBytes: ByteArray? = null,
    @ColumnInfo(name = "thunbnail_type") val thumbnailType: Int,
    @ColumnInfo(name = "updated_ms") val updatedMs: Long
) {
    fun toModel(): TokenInformation {
        return TokenInformation(
            tokenId,
            issuingBoxId,
            mintingTxId,
            displayName,
            description,
            decimals,
            fullSupply,
            reg7hex,
            reg8hex,
            reg9hex,
            genuineFlag,
            issuerLink,
            thumbnailBytes,
            thumbnailType,
            updatedMs
        )
    }
}

fun TokenInformation.toDbEntity(): TokenInformationDbEntity {
    return TokenInformationDbEntity(
        tokenId,
        issuingBoxId,
        mintingTxId,
        displayName,
        description,
        decimals,
        fullSupply,
        reg7hex,
        reg8hex,
        reg9hex,
        genuineFlag,
        issuerLink,
        thumbnailBytes,
        thumbnailType,
        updatedMs
    )
}