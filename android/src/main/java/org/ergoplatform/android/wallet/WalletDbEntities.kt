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
    @ColumnInfo(name = "wallet_type") val walletType: Int = WALLET_TYPE_P2PK,
    @ColumnInfo(name = "hide_balance") val hideBalance: Boolean = false,
) {
    fun toModel(): WalletConfig {
        return WalletConfig(
            id,
            displayName,
            firstAddress,
            encryptionType,
            secretStorage,
            unfoldTokens,
            extendedPublicKey,
            walletType,
            hideBalance
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
        extendedPublicKey,
        walletType,
        hideBalance
    )
}


@Entity(tableName = "wallet_states", indices = [Index("wallet_first_address")])
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

@Entity(tableName = "wallet_tokens", indices = [Index("wallet_first_address"), Index("public_address")])
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

@Entity(tableName = "wallet_addresses", indices = [Index("wallet_first_address")])
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

@Entity(tableName = "multisig_transactions", indices = [Index("wallet_first_address")])
data class MultisigTransactionDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    @ColumnInfo(name = "tx_id") val txId: String?,
    val state: Int,
    val memo: String?,
    @ColumnInfo(name = "last_json") val lastJson: String?,
    @ColumnInfo(name = "depends_on_tx_ids") val dependsOnTxIds: String?,
    @ColumnInfo(name = "last_change") val lastChange: Long
) {
    fun toModel(): MultisigTransaction {
        return MultisigTransaction(id, walletFirstAddress, txId, state, memo, lastJson, dependsOnTxIds, lastChange)
    }
}

fun MultisigTransaction.toDbEntity(): MultisigTransactionDbEntity {
    return MultisigTransactionDbEntity(
        id, walletFirstAddress, txId, state, memo, lastJson, dependsOnTxIds, lastChange
    )
}

@Entity(tableName = "multisig_participants", indices = [Index("wallet_first_address")])
data class MultisigParticipantDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    val address: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "has_signed") val hasSigned: Boolean = false
) {
    fun toModel(): MultisigParticipant {
        return MultisigParticipant(id, walletFirstAddress, address, orderIndex, hasSigned)
    }
}

fun MultisigParticipant.toDbEntity(): MultisigParticipantDbEntity {
    return MultisigParticipantDbEntity(
        id, walletFirstAddress, address, orderIndex, hasSigned
    )
}
