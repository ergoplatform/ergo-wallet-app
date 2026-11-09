package org.ergoplatform

import org.ergoplatform.transactions.SendTransactionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for broadcasting an already signed transaction over the preferred node and the
 * known nodes, without signing it again.
 */
class ErgoFacadeNodeFailoverTest {

    private val texts = TestStringProvider()

    private fun preferences(
        preferredNode: String,
        knownNodes: List<String> = emptyList()
    ): TestPreferencesProvider {
        return TestPreferencesProvider().apply {
            prefNodeUrl = preferredNode
            knownNodesList = knownNodes
        }
    }

    @Test
    fun `node candidates start with the preferred node`() {
        // The preferred node must always be tried before the known nodes.
        val candidates = ErgoFacade.getNodeCandidates(
            preferences("http://preferred:9053", listOf("http://node2:9053", "http://node3:9053"))
        )

        assertEquals(
            listOf("http://preferred:9053", "http://node2:9053", "http://node3:9053"),
            candidates
        )
    }

    @Test
    fun `node candidates are deduplicated and normalized`() {
        // Known nodes can be saved with a trailing slash or surrounding spaces, and they may
        // repeat the preferred node; all of them must collapse into one entry.
        val candidates = ErgoFacade.getNodeCandidates(
            preferences(
                "http://preferred:9053/",
                listOf("http://node2:9053/", "http://node2:9053", " http://node3:9053 ")
            )
        )

        assertEquals(
            listOf("http://preferred:9053", "http://node2:9053", "http://node3:9053"),
            candidates
        )
    }

    @Test
    fun `node candidates fall back to the preferred node only`() {
        // Without known nodes the behavior is the same as before the failover support.
        assertEquals(
            listOf("http://preferred:9053"),
            ErgoFacade.getNodeCandidates(preferences("http://preferred:9053/"))
        )
    }

    @Test
    fun `broadcast falls back to the next node when a node fails`() {
        // First node is down; the same signed bytes must be submitted to the next node.
        val attempts = mutableListOf<String>()

        val result = ErgoFacade.broadcastToNodes(
            listOf("http://node1:9053", "http://node2:9053"),
            texts
        ) { nodeUrl ->
            attempts += nodeUrl
            if (nodeUrl == "http://node1:9053") throw IllegalStateException("connection refused")
            SendTransactionResult(true, "tx-id-from-node2")
        }

        assertTrue(result.success)
        assertEquals("tx-id-from-node2", result.txId)
        assertEquals(listOf("http://node1:9053", "http://node2:9053"), attempts)
    }

    @Test
    fun `broadcast stops after the first successful node`() {
        // Later nodes must not be contacted once a node accepted the transaction.
        val attempts = mutableListOf<String>()

        val result = ErgoFacade.broadcastToNodes(
            listOf("http://node1:9053", "http://node2:9053"),
            texts
        ) { nodeUrl ->
            attempts += nodeUrl
            SendTransactionResult(true, "tx-id")
        }

        assertTrue(result.success)
        assertEquals("tx-id", result.txId)
        assertEquals(listOf("http://node1:9053"), attempts)
    }

    @Test
    fun `broadcast tries the next node when a node returns no transaction id`() {
        // An empty transaction id means the node did not accept the transaction.
        val attempts = mutableListOf<String>()

        val result = ErgoFacade.broadcastToNodes(
            listOf("http://node1:9053", "http://node2:9053"),
            texts
        ) { nodeUrl ->
            attempts += nodeUrl
            if (nodeUrl == "http://node1:9053") SendTransactionResult(false, errorMsg = "rejected")
            else SendTransactionResult(true, "tx-id")
        }

        assertTrue(result.success)
        assertEquals("tx-id", result.txId)
        assertEquals(listOf("http://node1:9053", "http://node2:9053"), attempts)
    }

    @Test
    fun `broadcast reports the last error when all nodes fail`() {
        // The user should see the error of the last attempted node.
        val result = ErgoFacade.broadcastToNodes(
            listOf("http://node1:9053", "http://node2:9053"),
            texts
        ) { nodeUrl ->
            throw IllegalStateException("$nodeUrl is down")
        }

        assertFalse(result.success)
        assertNotNull(result.errorMsg)
        assertTrue(result.errorMsg!!.contains("http://node2:9053 is down"))
    }

    @Test
    fun `broadcast fails when there is no node to connect to`() {
        // Defensive case: the candidate list is empty.
        val result = ErgoFacade.broadcastToNodes(emptyList(), texts) {
            SendTransactionResult(true, "tx-id")
        }

        assertFalse(result.success)
        assertNotNull(result.errorMsg)
    }
}
