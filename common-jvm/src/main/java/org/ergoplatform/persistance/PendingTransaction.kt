package org.ergoplatform.persistance

/**
 * Cross-platform model for a pending (unconfirmed) transaction tracked by the WAL.
 *
 * This class is platform-agnostic and lives in common-jvm. Platform-specific
 * persistence layers (Room on Android, SQLDelight on Desktop) map to/from this model.
 */
data class PendingTransaction(
    val id: Int = 0,
    val walletFirstAddress: String,
    val signingAddress: String,
    val txId: String? = null,
    val submittedAtMs: Long,
    val state: Int = STATE_SUBMITTING,
    val inputBoxIds: String,      // CSV of consumed box IDs
    val changeBoxIds: String,     // CSV of change output box IDs
    val outputBoxBytes: String,   // CSV of Base64-encoded serialized output boxes
    val ergDeltaNanoErg: Long,    // Negative = spent, positive = received
    val tokenDeltasJson: String   // Minimal JSON: {"tokenId":-50,"tokenId2":100}
) {
    companion object {
        const val STATE_SUBMITTING = 0
        const val STATE_SUBMITTED = 1
    }
}
