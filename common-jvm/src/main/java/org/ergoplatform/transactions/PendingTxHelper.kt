package org.ergoplatform.transactions

import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.SignedTransaction
import org.ergoplatform.deserializeErgobox
import org.ergoplatform.getErgoNetworkType
import org.ergoplatform.persistance.PendingTransaction
import org.ergoplatform.utils.Base64Coder

/**
 * Helper for building [PendingTransaction] WAL entries from signing results.
 *
 * All methods are stateless and compile-safe against ergo-appkit 44fddd97.
 */
object PendingTxHelper {

    /**
     * Build a [PendingTransaction] from a signed transaction and its original inputs.
     *
     * @param signedTx       The signed transaction (from AppKit)
     * @param serializedInputs  Original input boxes as byte arrays
     * @param walletFirstAddress The wallet's first (canonical) address
     * @param signingAddress    The address used to sign this transaction
     * @param walletAddresses   All addresses owned by this wallet (for delta calculation)
     */
    fun buildPendingTxFromSignedTx(
        signedTx: SignedTransaction,
        serializedInputs: List<ByteArray>,
        walletFirstAddress: String,
        signingAddress: String,
        walletAddresses: Set<String>
    ): PendingTransaction {
        val networkType = getErgoNetworkType()
        val now = System.currentTimeMillis()

        // ── Input analysis ──
        val inputBoxIds = mutableListOf<String>()
        var inputErgOurs = 0L
        val inputTokensOurs = mutableMapOf<String, Long>()

        for (inputBytes in serializedInputs) {
            val box = deserializeErgobox(inputBytes) ?: continue
            inputBoxIds.add(box.id.toString())

            val boxAddr = Address.fromErgoTree(box.ergoTree, networkType).toString()
            if (boxAddr in walletAddresses) {
                inputErgOurs += box.value
                for (token in box.tokens) {
                    val tid = token.id.toString()
                    inputTokensOurs[tid] = (inputTokensOurs[tid] ?: 0L) + token.value
                }
            }
        }

        // ── Output analysis ──
        val outputs = signedTx.outputsToSpend
        var outputErgOurs = 0L
        val outputTokensOurs = mutableMapOf<String, Long>()
        val changeBoxIds = mutableListOf<String>()
        val outputBoxBytesList = mutableListOf<String>()

        for (output in outputs) {
            val outputAddr = Address.fromErgoTree(output.ergoTree, networkType).toString()
            // Serialize output for WAL storage
            val ergoBoxBytes = output.bytes
            outputBoxBytesList.add(String(Base64Coder.encode(ergoBoxBytes)))

            if (outputAddr in walletAddresses) {
                outputErgOurs += output.value
                for (token in output.tokens) {
                    val tid = token.id.toString()
                    outputTokensOurs[tid] = (outputTokensOurs[tid] ?: 0L) + token.value
                }
                // Mark as change box (output owned by wallet)
                changeBoxIds.add(output.id.toString())
            }
        }

        // ── Deltas ──
        val ergDelta = outputErgOurs - inputErgOurs

        val allTokenIds = inputTokensOurs.keys + outputTokensOurs.keys
        val tokenDeltas = mutableMapOf<String, Long>()
        for (tid in allTokenIds) {
            val delta = (outputTokensOurs[tid] ?: 0L) - (inputTokensOurs[tid] ?: 0L)
            if (delta != 0L) tokenDeltas[tid] = delta
        }

        // ── Build minimal JSON for token deltas ──
        val tokenDeltasJson = if (tokenDeltas.isEmpty()) "{}" else {
            tokenDeltas.entries.joinToString(",", "{", "}") { (k, v) -> "\"$k\":$v" }
        }

        return PendingTransaction(
            walletFirstAddress = walletFirstAddress,
            signingAddress = signingAddress,
            submittedAtMs = now,
            txId = signedTx.id,  // Deterministic: Blake2b256 hash, known BEFORE broadcast
            state = PendingTransaction.STATE_SUBMITTING,
            inputBoxIds = inputBoxIds.joinToString(","),
            changeBoxIds = changeBoxIds.joinToString(","),
            outputBoxBytes = outputBoxBytesList.joinToString(","),
            ergDeltaNanoErg = ergDelta,
            tokenDeltasJson = tokenDeltasJson
        )
    }

    /**
     * Build a minimal [PendingTransaction] when full input box bytes are unavailable.
     * Used by ErgoPay flows where the dApp provides a pre-built ReducedTransaction.
     *
     * Provides blacklist protection (inputBoxIds extracted from TX JSON) but
     * no delta calculation (ergDelta=0, tokenDeltasJson="{}"). This is safe:
     * - Blacklist prevents double-spend (critical security)
     * - Zero delta means no UI balance adjustment (acceptable for ErgoPay)
     */
    fun buildMinimalPendingTx(
        signedTx: SignedTransaction,
        walletFirstAddress: String,
        signingAddress: String,
        walletAddresses: Set<String>
    ): PendingTransaction {
        val networkType = getErgoNetworkType()
        val now = System.currentTimeMillis()

        // Extract input box IDs from AppKit's typed API (no JSON parsing needed)
        val inputBoxIds = signedTx.signedInputs.map { it.id.toString() }

        // Output analysis (we DO have outputs from the signed TX)
        val outputs = signedTx.outputsToSpend
        val changeBoxIds = mutableListOf<String>()
        val outputBoxBytesList = mutableListOf<String>()

        for (output in outputs) {
            val ergoBoxBytes = output.bytes
            outputBoxBytesList.add(String(Base64Coder.encode(ergoBoxBytes)))
            val outputAddr = Address.fromErgoTree(output.ergoTree, networkType).toString()
            if (outputAddr in walletAddresses) {
                changeBoxIds.add(output.id.toString())
            }
        }

        return PendingTransaction(
            walletFirstAddress = walletFirstAddress,
            signingAddress = signingAddress,
            submittedAtMs = now,
            txId = signedTx.id,
            state = PendingTransaction.STATE_SUBMITTING,
            inputBoxIds = inputBoxIds.joinToString(","),
            changeBoxIds = changeBoxIds.joinToString(","),
            outputBoxBytes = outputBoxBytesList.joinToString(","),
            ergDeltaNanoErg = 0L,  // Unknown without input box values
            tokenDeltasJson = "{}"
        )
    }

    /**
     * Extract input box IDs from a [SignedTransaction] using the type-safe
     * AppKit API ([SignedTransaction.getSignedInputs]).
     *
     * Replaces the previous Gson JSON parsing approach, eliminating the
     * runtime dependency on JSON field names and the Gson library.
     */
    internal fun extractInputBoxIds(signedTx: SignedTransaction): List<String> {
        return try {
            signedTx.signedInputs.map { it.id.toString() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Deserialize output boxes from WAL CSV format (Base64-encoded, comma-separated).
     *
     * @return List of (boxId, rawBytes) pairs
     */
    fun deserializeOutputBoxes(csvBase64: String): List<Pair<String, ByteArray>> {
        if (csvBase64.isBlank()) return emptyList()
        val result = mutableListOf<Pair<String, ByteArray>>()
        for (b64 in csvBase64.split(",")) {
            if (b64.isBlank()) continue
            try {
                val bytes = Base64Coder.decode(b64)
                val box = deserializeErgobox(bytes)
                if (box != null) {
                    result.add(box.id.toString() to bytes)
                }
            } catch (_: Throwable) {
                // Skip malformed entries
            }
        }
        return result
    }

    /**
     * Aggregate token deltas from multiple pending transactions.
     * Parses the minimal JSON format: {"tokenId1":-50,"tokenId2":100}
     *
     * @param jsonFragments List of tokenDeltasJson strings from the DAO
     * @return Merged map of tokenId → total delta (negative = spent)
     */
    fun aggregateTokenDeltas(jsonFragments: List<String>): Map<String, Long> {
        val merged = mutableMapOf<String, Long>()
        for (json in jsonFragments) {
            if (json.isBlank() || json == "{}") continue
            try {
                json.trim('{', '}').split(",").forEach { entry ->
                    if (entry.isBlank()) return@forEach
                    val parts = entry.split(":", limit = 2)
                    if (parts.size != 2) return@forEach

                    val tokenId = parts[0].trim('"')
                    val delta = parts[1].toLongOrNull() ?: return@forEach
                    merged[tokenId] = (merged[tokenId] ?: 0L) + delta
                }
            } catch (_: Throwable) {
                // Malformed fragment → skip silently, UI must never crash
            }
        }
        return merged
    }
}
