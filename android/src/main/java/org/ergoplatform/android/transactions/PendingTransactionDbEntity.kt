package org.ergoplatform.android.transactions

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.ergoplatform.persistance.PendingTransaction

@Entity(
    tableName = "pending_transactions",
    indices = [
        Index(value = ["wallet_first_address"]),
        Index(value = ["signing_address"])
    ]
)
data class PendingTransactionDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "wallet_first_address") val walletFirstAddress: String,
    @ColumnInfo(name = "signing_address") val signingAddress: String,
    @ColumnInfo(name = "submitted_at_ms") val submittedAtMs: Long,
    @ColumnInfo(name = "tx_id") val txId: String? = null,
    @ColumnInfo(name = "state") val state: Int = 0,
    @ColumnInfo(name = "input_box_ids") val inputBoxIds: String,
    @ColumnInfo(name = "change_box_ids") val changeBoxIds: String,
    @ColumnInfo(name = "output_box_bytes") val outputBoxBytes: String,
    @ColumnInfo(name = "erg_delta_nanoerg") val ergDeltaNanoErg: Long,
    @ColumnInfo(name = "token_deltas_json") val tokenDeltasJson: String,
) {
    fun toModel(): PendingTransaction {
        return PendingTransaction(
            id = id,
            walletFirstAddress = walletFirstAddress,
            signingAddress = signingAddress,
            submittedAtMs = submittedAtMs,
            txId = txId,
            state = state,
            inputBoxIds = inputBoxIds,
            changeBoxIds = changeBoxIds,
            outputBoxBytes = outputBoxBytes,
            ergDeltaNanoErg = ergDeltaNanoErg,
            tokenDeltasJson = tokenDeltasJson,
        )
    }
}

fun PendingTransaction.toDbEntity() = PendingTransactionDbEntity(
    id = id,
    walletFirstAddress = walletFirstAddress,
    signingAddress = signingAddress,
    submittedAtMs = submittedAtMs,
    txId = txId,
    state = state,
    inputBoxIds = inputBoxIds,
    changeBoxIds = changeBoxIds,
    outputBoxBytes = outputBoxBytes,
    ergDeltaNanoErg = ergDeltaNanoErg,
    tokenDeltasJson = tokenDeltasJson,
)
