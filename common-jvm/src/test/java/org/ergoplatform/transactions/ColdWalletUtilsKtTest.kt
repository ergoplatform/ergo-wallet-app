package org.ergoplatform.transactions

import org.ergoplatform.android.transactions.*
import org.junit.Assert
import org.junit.Test

class ColdWalletUtilsKtTest {

    @Test
    fun testRoundTrip() {
        val coldSigningRequest = PromptSigningResult(
            true, "DUMMYDATA".toByteArray(), listOf(
                "INPUT1".toByteArray()
            ), "ADDRESS"
        )

        val csr = buildColdSigningRequest(coldSigningRequest)

        Assert.assertNotNull(csr)

        val manyChunks = coldSigninRequestToQrChunks(csr!!, 30)
        val oneChunk = coldSigninRequestToQrChunks(csr!!, 50000000)
        Assert.assertEquals(1, oneChunk.size)
        Assert.assertEquals(0, getColdSigingRequestChunkIndex(oneChunk.first()))
        Assert.assertEquals(1, getColdSigingRequestChunkPagesCount(oneChunk.first()))
        Assert.assertEquals(manyChunks.size, getColdSigingRequestChunkPagesCount(manyChunks.first()))

        val prompt1 = coldSigningRequestFromQrChunks(manyChunks)
        val prompt2 = coldSigningRequestFromQrChunks(oneChunk)

        Assert.assertEquals(coldSigningRequest.address, prompt1.address)
        Assert.assertEquals(coldSigningRequest.address, prompt2.address)
        Assert.assertArrayEquals(coldSigningRequest.serializedTx, prompt1.serializedTx)
        Assert.assertArrayEquals(coldSigningRequest.serializedTx, prompt2.serializedTx)
        Assert.assertArrayEquals(coldSigningRequest.serializedInputs!!.first(), prompt1.serializedInputs!!.first())
        Assert.assertArrayEquals(coldSigningRequest.serializedInputs!!.first(), prompt2.serializedInputs!!.first())
    }
}