package org.ergoplatform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class NodeFallbackTest {

    @Test
    fun `preferred node is first and equivalent URLs are de-duplicated`() {
        assertEquals(
            listOf(
                "https://preferred.example/",
                "https://backup-one.example/",
                "https://backup-two.example/"
            ),
            buildNodeFallbackList(
                "https://preferred.example/",
                listOf(
                    "https://backup-one.example",
                    "https://preferred.example",
                    " ",
                    "https://backup-two.example///"
                )
            )
        )
    }

    @Test
    fun `preferred node success does not contact fallbacks`() {
        val attemptedNodes = mutableListOf<String>()

        val result = executeWithNodeFallback(
            listOf("preferred", "backup")
        ) { nodeUrl ->
            attemptedNodes.add(nodeUrl)
            "transaction-id"
        }

        assertEquals("transaction-id", result)
        assertEquals(listOf("preferred"), attemptedNodes)
    }

    @Test
    fun `connection failure tries the next node`() {
        val attemptedNodes = mutableListOf<String>()

        val result = executeWithNodeFallback(
            listOf("preferred", "backup")
        ) { nodeUrl ->
            attemptedNodes.add(nodeUrl)
            if (nodeUrl == "preferred") {
                throw IllegalStateException("request failed", IOException("connection refused"))
            }
            "transaction-id"
        }

        assertEquals("transaction-id", result)
        assertEquals(listOf("preferred", "backup"), attemptedNodes)
    }

    @Test
    fun `last connection failure is preserved when every node fails`() {
        val preferredFailure = IOException("preferred unavailable")
        val backupFailure = IOException("backup unavailable")

        try {
            executeWithNodeFallback<String>(listOf("preferred", "backup")) { nodeUrl ->
                throw if (nodeUrl == "preferred") preferredFailure else backupFailure
            }
            fail("Expected the last node failure")
        } catch (actual: Throwable) {
            assertSame(backupFailure, actual)
        }
    }

    @Test
    fun `transaction rejection does not retry another node`() {
        val attemptedNodes = mutableListOf<String>()
        val rejection = IllegalStateException("transaction rejected")

        try {
            executeWithNodeFallback<String>(listOf("preferred", "backup")) { nodeUrl ->
                attemptedNodes.add(nodeUrl)
                throw rejection
            }
            fail("Expected the transaction rejection")
        } catch (actual: Throwable) {
            assertSame(rejection, actual)
        }

        assertEquals(listOf("preferred"), attemptedNodes)
    }
}
