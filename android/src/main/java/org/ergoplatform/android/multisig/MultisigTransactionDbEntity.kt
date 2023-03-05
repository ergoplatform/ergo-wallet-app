package org.ergoplatform.android.multisig

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.ergoplatform.persistance.MultisigTransaction

@Entity(
    tableName = "multisig_transaction",
    indices = [Index(
        value = arrayOf("address", "last_change"),
        orders = arrayOf(Index.Order.ASC, Index.Order.DESC)
    )]
)
data class MultisigTransactionDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val address: String,
    @ColumnInfo(name = "tx_id") val txId: String,
    @ColumnInfo(name = "last_change") val lastChange: Long,
    val memo: String?,
    val data: String?,
    val state: Int,
) {
    fun toModel() = MultisigTransaction(
        id, address, txId, lastChange, memo, data, state
    )
}

fun MultisigTransaction.toDbEntity() =
    MultisigTransactionDbEntity(
        id, address, txId, lastChange, memo, data, state
    )