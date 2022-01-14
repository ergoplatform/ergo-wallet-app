package org.ergoplatform

import org.junit.Assert.*
import org.junit.Test

class ErgoPayTest {

    @Test
    fun parseErgoPaySigningRequestFromUriTest() {
        isErgoMainNet = false

        val uri =
            "ergoPay:9AEBJmPrKJW357sXF3WsClhAxbyYRt1quzw3ch4Vy5sclX4AAAAAA4CU69wDAAjNAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE6o4oFAADAhD0QBQQABAAONhACBJABCM0Ceb5mfvncu6xVoGKVzocLBwKb_NstzijZWfKBWxb4F5jqAtGSo5qMx6cBcwBzARABAgQC0ZaDAwGTo4zHsqVzAAABk8KypXMBAHRzAnMDgwEIze6sk7GlcwSjigUAAOaW4NLqAQAIzQKDM_n3RU-NX_c9usmDN2ftb8OobPCnPflGsy6pkn2Rl6OKBQAAzQKDM_n3RU-NX_c9usmDN2ftb8OobPCnPflGsy6pkn2Rl51PjGA="

        assertTrue(isErgoPaySigningRequest(uri))
        assertFalse(isErgoPaySigningRequest(""))

        val ergoPaySigningRequest = parseErgoPaySigningRequestFromUri(uri)
        ergoPaySigningRequest.apply {
            assertNotNull(reducedTx)
            assertNull(message)
            assertNull(replyToUrl)
            assertNull(p2pkAddress)
            assertEquals(MessageSeverity.NONE, messageSeverity)

            val parsedTx = deserializeUnsignedTxOffline(reducedTx!!)
            assertNotNull(parsedTx)
        }

        // this will fetch information from ergo explorer, commented since server problems would break build
        // val txInfo = ergoPaySigningRequest.buildTransactionInfo(ErgoApiService.getOrInit(TestPreferencesProvider()))
        // assertNotNull(txInfo)
    }
}