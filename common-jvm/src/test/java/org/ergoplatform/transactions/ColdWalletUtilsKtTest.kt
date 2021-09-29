package org.ergoplatform.transactions

import org.ergoplatform.android.ergoNetworkType
import org.ergoplatform.android.transactions.*
import org.ergoplatform.appkit.NetworkType
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
        Assert.assertEquals(
            manyChunks.size,
            getColdSigingRequestChunkPagesCount(manyChunks.first())
        )

        val prompt1 = coldSigningRequestFromQrChunks(manyChunks)
        val prompt2 = coldSigningRequestFromQrChunks(oneChunk)

        Assert.assertEquals(coldSigningRequest.address, prompt1.address)
        Assert.assertEquals(coldSigningRequest.address, prompt2.address)
        Assert.assertArrayEquals(coldSigningRequest.serializedTx, prompt1.serializedTx)
        Assert.assertArrayEquals(coldSigningRequest.serializedTx, prompt2.serializedTx)
        Assert.assertArrayEquals(
            coldSigningRequest.serializedInputs!!.first(),
            prompt1.serializedInputs!!.first()
        )
        Assert.assertArrayEquals(
            coldSigningRequest.serializedInputs!!.first(),
            prompt2.serializedInputs!!.first()
        )
    }

    @Test
    fun buildTransactionInfoTest() {
        ergoNetworkType = NetworkType.TESTNET
        val csr =
            parseColdSigningRequest("{\"reducedTx\":\"9AEBJmPrKJW357sXF3WsClhAxbyYRt1quzw3ch4Vy5sclX4AAAAAA4CU69wDAAjNAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE6o4oFAADAhD0QBQQABAAONhACBJABCM0Ceb5mfvncu6xVoGKVzocLBwKb/NstzijZWfKBWxb4F5jqAtGSo5qMx6cBcwBzARABAgQC0ZaDAwGTo4zHsqVzAAABk8KypXMBAHRzAnMDgwEIze6sk7GlcwSjigUAAOaW4NLqAQAIzQKDM/n3RU+NX/c9usmDN2ftb8OobPCnPflGsy6pkn2Rl6OKBQAAzQKDM/n3RU+NX/c9usmDN2ftb8OobPCnPflGsy6pkn2Rl51PjGA\\u003d\",\"sender\":\"3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt\",\"inputs\":[\"pq+IsO4BAAjNAoMz+fdFT41f9z26yYM3Z+1vw6hs8Kc9+UazLqmSfZGXneUEAABT3vrcze/5EGY0QKBNfYn0USYWLqxfTf32VmfH2yml/wA\\u003d\"]}")

        val ti = buildTransactionInfoFromReduced(csr.serializedTx!!, csr.serializedInputs)

        Assert.assertNotNull(ti.outputs)
        Assert.assertEquals(1, ti.inputs.size)
        Assert.assertEquals(3, ti.outputs.size)
    }
}