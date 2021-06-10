package org.ergoplatform.android.wallet

import androidx.room.*

val ENC_TYPE_PASSWORD = 1
val ENC_TYPE_DEVICE = 2

@Entity(tableName = "wallet_configs")
data class WalletConfigDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "public_address") val publicAddress: String?,
    @ColumnInfo(name = "enc_type") val encryptionType: Int?,
    @ColumnInfo(name = "secret_storage") val secretStorage: ByteArray?,
)

@Entity(tableName = "wallet_states")
data class WalletStateDbEntity(
    @PrimaryKey @ColumnInfo(name = "wallet_id") val walletId: Int,
    val transactions: Int?,
    val balance: Long?,
    @ColumnInfo(name = "unconfirmed_balance") val unconfirmedBalance: Long?
)

data class WalletDbEntity(
    @Embedded val walletConfig: WalletConfigDbEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "wallet_id"
    )
    val state: WalletStateDbEntity?
)
