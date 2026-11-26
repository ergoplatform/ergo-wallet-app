package org.ergoplatform.transactions

import org.junit.Assert.*
import org.junit.Test

class PendingTxHelperTest {

    // ══════════════════════════════════════════════════════════════
    // aggregateTokenDeltas
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `aggregateTokenDeltas - single fragment`() {
        val result = PendingTxHelper.aggregateTokenDeltas(
            listOf("""{"abc123":-50,"def456":100}""")
        )
        assertEquals(mapOf("abc123" to -50L, "def456" to 100L), result)
    }

    @Test
    fun `aggregateTokenDeltas - merges across fragments`() {
        val result = PendingTxHelper.aggregateTokenDeltas(
            listOf(
                """{"tokenA":-50}""",
                """{"tokenA":-30,"tokenB":200}"""
            )
        )
        assertEquals(-80L, result["tokenA"])
        assertEquals(200L, result["tokenB"])
    }

    @Test
    fun `aggregateTokenDeltas - empty fragments`() {
        assertEquals(emptyMap<String, Long>(), PendingTxHelper.aggregateTokenDeltas(emptyList()))
        assertEquals(emptyMap<String, Long>(), PendingTxHelper.aggregateTokenDeltas(listOf("{}")))
        assertEquals(emptyMap<String, Long>(), PendingTxHelper.aggregateTokenDeltas(listOf("")))
        assertEquals(emptyMap<String, Long>(), PendingTxHelper.aggregateTokenDeltas(listOf("  ")))
    }

    @Test
    fun `aggregateTokenDeltas - malformed JSON never crashes`() {
        // These should all be silently skipped
        val result = PendingTxHelper.aggregateTokenDeltas(
            listOf(
                "not json at all",
                "{malformed",
                """{"goodToken":42}""",  // This one should work
                """{"badValue":"notANumber"}""",  // Should skip this entry
                """{"trailing":100,}""",  // Trailing comma
                """{"":100}""",  // Empty key
            )
        )
        // Only the valid entry should survive
        assertTrue(result.containsKey("goodToken"))
        assertEquals(42L, result["goodToken"])
    }

    @Test
    fun `aggregateTokenDeltas - net zero delta excluded`() {
        val result = PendingTxHelper.aggregateTokenDeltas(
            listOf(
                """{"tokenX":100}""",
                """{"tokenX":-100}"""
            )
        )
        // Net delta is 0 — should still be in map (aggregation doesn't filter zeros)
        assertEquals(0L, result["tokenX"])
    }

    @Test
    fun `aggregateTokenDeltas - large values`() {
        val result = PendingTxHelper.aggregateTokenDeltas(
            listOf("""{"bigToken":${Long.MAX_VALUE}}""")
        )
        assertEquals(Long.MAX_VALUE, result["bigToken"])
    }

    // ══════════════════════════════════════════════════════════════
    // deserializeOutputBoxes
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `deserializeOutputBoxes - empty input`() {
        assertEquals(emptyList<Pair<String, ByteArray>>(), PendingTxHelper.deserializeOutputBoxes(""))
        assertEquals(emptyList<Pair<String, ByteArray>>(), PendingTxHelper.deserializeOutputBoxes("  "))
    }

    @Test
    fun `deserializeOutputBoxes - malformed base64 never crashes`() {
        val result = PendingTxHelper.deserializeOutputBoxes("not-valid-base64,also-bad,,,")
        // Should return empty list (no valid boxes)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deserializeOutputBoxes - valid base64 but invalid box bytes`() {
        // Valid base64, but not a valid ErgoBox serialization
        val result = PendingTxHelper.deserializeOutputBoxes("AAAA,AQID")
        assertTrue(result.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════
    // extractInputBoxIds — Type-Safe AppKit API (no JSON parsing)
    // ══════════════════════════════════════════════════════════════
    // The old JSON-based tests (extractInputBoxIdsFromJson) were removed
    // because the Gson parsing was replaced with SignedTransaction.getSignedInputs().
    // The new method is compile-time type-safe: if AppKit changes the API,
    // the build fails immediately instead of silently returning empty lists.
    // Integration coverage is provided by buildMinimalPendingTx() callers.
}
