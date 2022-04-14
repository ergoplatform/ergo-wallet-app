package org.ergoplatform.android.transactions

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.persistance.AddressTransaction
import org.ergoplatform.persistance.AddressTransactionToken

@Entity(
    tableName = "address_transaction",
    indices = [Index(
        value = arrayOf("address", "inclusion_height"),
        orders = arrayOf(Index.Order.ASC, Index.Order.DESC)
    )]
)
data class AddressTransactionDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val address: String,
    @ColumnInfo(name = "tx_id") val txId: String,
    @ColumnInfo(name = "inclusion_height") val inclusionHeight: Long,
    val timestamp: Long,
    @ColumnInfo(name = "nanoerg") val nanoErg: Long,
    val message: String?,
    val state: Int,
) {
    fun toModel(): AddressTransaction {
        return AddressTransaction(
            id, address, txId, inclusionHeight, timestamp, ErgoAmount(nanoErg), message, state
        )
    }
}

fun AddressTransaction.toDbEntity() =
    AddressTransactionDbEntity(
        id,
        address,
        txId,
        inclusionHeight,
        timestamp,
        ergAmount.nanoErgs,
        message,
        state
    )

@Entity(
    tableName = "address_transaction_token",
    indices = [Index(
        value = arrayOf("address", "tx_id"),
    )]
)
data class AddressTransactionTokenDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val address: String,
    @ColumnInfo(name = "tx_id") val txId: String,
    @ColumnInfo(name = "token_id") val tokenId: String,
    val name: String,
    @ColumnInfo(name = "amount") val rawAmount: Long,
    val decimals: Int,
) {
    fun toModel(): AddressTransactionToken {
        return AddressTransactionToken(
            id,
            address,
            txId,
            tokenId,
            name,
            TokenAmount(rawAmount, decimals)
        )
    }
}

fun AddressTransactionToken.toDbEntity() = AddressTransactionTokenDbEntity(
    id,
    address,
    txId,
    tokenId,
    name,
    tokenAmount.rawValue,
    tokenAmount.decimals
)