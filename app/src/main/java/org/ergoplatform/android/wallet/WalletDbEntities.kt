package org.ergoplatform.android.wallet

import androidx.room.*

val ENC_TYPE_PASSWORD = 1
val ENC_TYPE_DEVICE = 2

@Entity(tableName = "wallet_configs")
data class WalletConfigDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "public_address") val firstAddress: String?,
    @ColumnInfo(name = "enc_type") val encryptionType: Int?,
    @ColumnInfo(name = "secret_storage") val secretStorage: ByteArray?,
    @ColumnInfo(name = "unfold_tokens") val unfoldTokens: Boolean = false,
)

@Entity(tableName = "wallet_states")
data class WalletStateDbEntity(
    @PrimaryKey @ColumnInfo(name = "public_address") val publicAddress: String,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    val balance: Long?,
    @ColumnInfo(name = "unconfirmed_balance") val unconfirmedBalance: Long?
)

@Entity(tableName = "wallet_tokens")
data class WalletTokenDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "public_address") val publicAddress: String,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    @ColumnInfo(name = "token_id") val tokenId: String?,
    val amount: Long?,
    val decimals: Int?,
    val name: String?,
)

@Entity(tableName = "wallet_addresses")
data class WalletAddressDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    @ColumnInfo(name = "index") val derivationIndex: Int,
    @ColumnInfo(name = "public_address") val publicAddress: String,
    val label: String?,
)

data class WalletDbEntity(
    @Embedded val walletConfig: WalletConfigDbEntity,
    @Relation(
        parentColumn = "public_address",
        entityColumn = "wallet_first_address"
    )
    val state: List<WalletStateDbEntity>,
    @Relation(
        parentColumn = "public_address",
        entityColumn = "wallet_first_address"
    )
    val tokens: List<WalletTokenDbEntity>,
    @Relation(
        parentColumn = "public_address",
        entityColumn = "wallet_first_address",
    )
    val addresses: List<WalletAddressDbEntity>
)
