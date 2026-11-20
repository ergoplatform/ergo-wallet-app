package org.ergoplatform

import junit.framework.TestCase

class ErgoFacadeNodeRetryTest : TestCase() {

    fun testBuildNodeBroadcastSequenceKeepsPreferredFirstAndDeduplicates() {
        assertEquals(
            listOf("https://preferred.example", "https://fallback.example"),
            buildNodeBroadcastSequence(
                " https://preferred.example/ ",
                listOf(
                    "https://preferred.example",
                    "https://fallback.example/",
                    "",
                    "https://fallback.example"
                )
            )
        )
    }

    fun testSendToFirstAvailableNodeFallsBackInOrder() {
        val attemptedNodes = mutableListOf<String>()

        val result = sendToFirstAvailableNode(listOf("preferred", "fallback", "unused")) {
            attemptedNodes.add(it)
            if (it == "preferred") throw IllegalStateException("unavailable")
            "tx-id"
        }

        assertEquals("tx-id", result)
        assertEquals(listOf("preferred", "fallback"), attemptedNodes)
    }

    fun testSendToFirstAvailableNodeStopsAfterPreferredSucceeds() {
        val attemptedNodes = mutableListOf<String>()

        val result = sendToFirstAvailableNode(listOf("preferred", "fallback")) {
            attemptedNodes.add(it)
            "tx-id"
        }

        assertEquals("tx-id", result)
        assertEquals(listOf("preferred"), attemptedNodes)
    }

    fun testSendToFirstAvailableNodeRethrowsLastFailure() {
        val firstFailure = IllegalStateException("first")
        val lastFailure = IllegalArgumentException("last")

        try {
            sendToFirstAvailableNode(listOf("preferred", "fallback")) {
                if (it == "preferred") throw firstFailure else throw lastFailure
            }
            fail("Expected the final node failure")
        } catch (t: Throwable) {
            assertSame(lastFailure, t)
        }
    }
}
