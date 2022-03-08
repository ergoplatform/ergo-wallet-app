package org.ergoplatform.android.wallet

import androidx.room.*
import org.ergoplatform.persistance.*

// Defines the Room database entities for wallets
// Modularization is done similar to
// https://jacquessmuts.github.io/post/modularization_room/

@Entity(tableName = "wallet_configs")
data class WalletConfigDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "public_address") val firstAddress: String?,
    @ColumnInfo(name = "enc_type") val encryptionType: Int?,
    @ColumnInfo(name = "secret_storage") val secretStorage: ByteArray?,
    @ColumnInfo(name = "unfold_tokens") val unfoldTokens: Boolean = false,
    @ColumnInfo(name = "xpubkey") val extendedPublicKey: String? = null,
) {
    fun toModel(): WalletConfig {
        return WalletConfig(
            id,
            displayName,
            firstAddress,
            encryptionType,
            secretStorage,
            unfoldTokens,
            extendedPublicKey
        )
    }
}

fun WalletConfig.toDbEntity(): WalletConfigDbEntity {
    return WalletConfigDbEntity(
        id,
        displayName,
        firstAddress,
        encryptionType,
        secretStorage,
        unfoldTokens,
        extendedPublicKey
    )
}


@Entity(tableName = "wallet_states")
data class WalletStateDbEntity(
    @PrimaryKey @ColumnInfo(name = "public_address") val publicAddress: String,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    val balance: Long?,
    @ColumnInfo(name = "unconfirmed_balance") val unconfirmedBalance: Long?
) {
    fun toModel(): WalletState {
        return WalletState(publicAddress, walletFirstAddress, balance, unconfirmedBalance)
    }
}

fun WalletState.toDbEntity(): WalletStateDbEntity {
    return WalletStateDbEntity(publicAddress, walletFirstAddress, balance, unconfirmedBalance)
}

@Entity(tableName = "wallet_tokens")
data class WalletTokenDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "public_address") val publicAddress: String,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    @ColumnInfo(name = "token_id") val tokenId: String?,
    val amount: Long?,
    val decimals: Int?,
    val name: String?,
) {
    fun toModel(): WalletToken {
        return WalletToken(
            id.toLong(),
            publicAddress,
            walletFirstAddress,
            tokenId,
            amount,
            decimals ?: 0,
            name
        )
    }
}

fun WalletToken.toDbEntity(): WalletTokenDbEntity {
    return WalletTokenDbEntity(
        id.toInt(),
        publicAddress,
        walletFirstAddress,
        tokenId,
        amount,
        decimals,
        name
    )
}

@Entity(tableName = "wallet_addresses")
data class WalletAddressDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    @ColumnInfo(name = "index") val derivationIndex: Int,
    @ColumnInfo(name = "public_address") val publicAddress: String,
    val label: String?,
) {
    fun toModel(): WalletAddress {
        return WalletAddress(id.toLong(), walletFirstAddress, derivationIndex, publicAddress, label)
    }
}

fun WalletAddress.toDbEntity(): WalletAddressDbEntity {
    return WalletAddressDbEntity(
        id.toInt(),
        walletFirstAddress,
        derivationIndex,
        publicAddress,
        label
    )
}

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
) {
    fun toModel(): Wallet {
        return Wallet(
            walletConfig.toModel(),
            state.map { it.toModel() },
            tokens.map { it.toModel() },
            addresses.map { it.toModel() })
    }
}
