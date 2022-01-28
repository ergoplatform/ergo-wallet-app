package org.ergoplatform.transactions

import org.ergoplatform.isErgoMainNet
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

        val manyChunks = coldSigningRequestToQrChunks(csr!!, 50)
        val oneChunk = coldSigningRequestToQrChunks(csr, 50000000)
        Assert.assertEquals(1, oneChunk.size)
        Assert.assertEquals(1, getColdSigningRequestChunk(oneChunk.first())?.index)
        Assert.assertEquals(1, getColdSigningRequestChunk(manyChunks.first())?.index)
        Assert.assertEquals(1, getColdSigningRequestChunk(oneChunk.first())?.pages)
        Assert.assertEquals(manyChunks.size, getColdSigningRequestChunk(manyChunks.first())?.pages)

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
        isErgoMainNet = false
        val csr =
            parseColdSigningRequest("{\"reducedTx\":\"9AEBJmPrKJW357sXF3WsClhAxbyYRt1quzw3ch4Vy5sclX4AAAAAA4CU69wDAAjNAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE6o4oFAADAhD0QBQQABAAONhACBJABCM0Ceb5mfvncu6xVoGKVzocLBwKb/NstzijZWfKBWxb4F5jqAtGSo5qMx6cBcwBzARABAgQC0ZaDAwGTo4zHsqVzAAABk8KypXMBAHRzAnMDgwEIze6sk7GlcwSjigUAAOaW4NLqAQAIzQKDM/n3RU+NX/c9usmDN2ftb8OobPCnPflGsy6pkn2Rl6OKBQAAzQKDM/n3RU+NX/c9usmDN2ftb8OobPCnPflGsy6pkn2Rl51PjGA\\u003d\",\"sender\":\"3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt\",\"inputs\":[\"pq+IsO4BAAjNAoMz+fdFT41f9z26yYM3Z+1vw6hs8Kc9+UazLqmSfZGXneUEAABT3vrcze/5EGY0QKBNfYn0USYWLqxfTf32VmfH2yml/wA\\u003d\"]}")

        val ti = csr.buildTransactionInfo()

        Assert.assertNotNull(ti.outputs)
        Assert.assertEquals(1, ti.inputs.size)
        Assert.assertEquals(3, ti.outputs.size)

        Assert.assertEquals(2, ti.reduceBoxes().outputs.size)

        val reducedTx2 =
            "0QMDCp28G2Gct69t0D+IMK8h0kKEjj7f49cAtGwvUtSOPesAAGPk+W6yjBzn2jPcoFiaufhsXy52IsuIQjiCCE/q8m9DAACuLv3AeVnq+cluQls7Kz/8+YsWGTDNCl9HiVzLMP4oNgAAAARRQIOhcPxzQHHAd0j/RElAYGZUMXvVFoZRIO1wKVKrG/n/BLk/9n7C3gSwVnXkGjjA+vQJ1Jn9NExtNOXppL7dzaVi/TpNypHLwgdakRo6WxG37YEAl7GsWlS3Yqh3sGKW1lsZOJnBVNdbw6VbLTwjEfCfArnW7S2skTyxDDgwgAOA3qDLBQAIzQKDM/n3RU+NX/c9usmDN2ftb8OobPCnPflGsy6pkn2Rl7WLBQEA8zkAwIQ9EAUEAAQADjYQAgSQAQjNAnm+Zn753LusVaBilc6HCwcCm/zbLc4o2VnygVsW+BeY6gLRkqOajMenAXMAcwEQAQIEAtGWgwMBk6OMx7KlcwAAAZPCsqVzAQB0cwJzA4MBCM3urJOxpXMEtYsFAADA7/HRyQIACM0CLecmo/oGlC3SIpWWYFq5MkBM44Y0Rxm0lh3tkwZ7ITq1iwUEAQECgNDbw/QCAPCB28P0AgPQ2JatAwDNAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE6nU/NAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE6nU/NAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE6nU/QjAE\\u003d"
        val csr2 =
            parseColdSigningRequest(
                "{\"reducedTx\":\"$reducedTx2\",\"sender\":\"3WvxRdGA2Ce3otzqtc7jUb61H67NiugArk9mTCxKwMQjrKgsjwwj\",\"inputs\":[\"gJTr3AMACM0CLecmo/oGlC3SIpWWYFq5MkBM44Y0Rxm0lh3tkwZ7ITqlywQAAGRGq63RnvOuuPeM07c0y1ZzwTcilEjMaNkS/Ws2WuLVAA\\u003d\\u003d\",\"gJTr3AMACM0CLecmo/oGlC3SIpWWYFq5MkBM44Y0Rxm0lh3tkwZ7ITrk5wQAAPRQZ9K80uxdbpd1Zwx4DA1zakyULHJh/yfOl8ocdz6uAA\\u003d\\u003d\",\"gKr548cCAAjNAi3nJqP6BpQt0iKVlmBauTJATOOGNEcZtJYd7ZMGeyE67OcEBPn/BLk/9n7C3gSwVnXkGjjA+vQJ1Jn9NExtNOXppL7dAc2lYv06TcqRy8IHWpEaOlsRt+2BAJexrFpUt2Kod7BigNDbw/QCUUCDoXD8c0BxwHdI/0RJQGBmVDF71RaGUSDtcClSqxvju9vD9AKW1lsZOJnBVNdbw6VbLTwjEfCfArnW7S2skTyxDDgwgNDYlq0DANXS4I5Ac3ygmxQ6/8SYHikzLoKK0T4jDBvcoZVjWvIvAg\\u003d\\u003d\"]}"
            )
        val ti2 = csr2.buildTransactionInfo()

        Assert.assertNotNull(ti2.outputs)
        Assert.assertEquals(3, ti2.inputs.size)
        Assert.assertEquals(3, ti2.outputs.size)
        Assert.assertEquals(4, ti2.outputs.last().assets.size)

        val tokenAmountBeforeReduce = ti2.outputs.last().assets.last().amount

        val ti2b = ti2.reduceBoxes()
        Assert.assertEquals(1, ti2b.inputs.size)
        Assert.assertEquals(2, ti2b.outputs.size)

        // Check that neither original token list nor original token object was changed by reduceBoxes()
        Assert.assertEquals(4, ti2.outputs.last().assets.size)
        Assert.assertEquals(tokenAmountBeforeReduce, ti2.outputs.last().assets.last().amount)

        // exception raised without input boxes
        val csr3 =
            parseColdSigningRequest("{\"reducedTx\":\"$reducedTx2\"}")

        var exceptionThrown = false
        try {
            csr3.buildTransactionInfo()
        } catch (t: Throwable) {
            exceptionThrown = true
        }
        Assert.assertTrue(exceptionThrown)
    }
}