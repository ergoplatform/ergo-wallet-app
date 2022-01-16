package org.ergoplatform.transactions

import org.ergoplatform.appkit.Parameters
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.parsePaymentRequest
import org.junit.Assert
import org.junit.Test

class PaymentRequestKtTest {

    @Test
    fun parseContentFromQrCode() {
        isErgoMainNet = true

        val parse1 =
            parsePaymentRequest("https://explorer.ergoplatform.com/payment-request?address=testaddr&amount=1.0&tokenId=notNumericAmount")

        Assert.assertEquals(Parameters.OneErg, parse1?.amount?.nanoErgs)
        Assert.assertEquals("testaddr", parse1?.address)
        Assert.assertEquals(0, parse1?.tokens?.size)

        val parse2 =
            parsePaymentRequest("https://explorer.ergoplatform.com/payment-request?address=testaddr&amount=2&12345=22.3")

        Assert.assertEquals(2 * Parameters.OneErg, parse2?.amount?.nanoErgs)
        Assert.assertEquals("testaddr", parse2?.address)
        Assert.assertEquals(1, parse2?.tokens?.size)
        Assert.assertEquals("12345", parse2?.tokens?.keys?.first())
        Assert.assertEquals("22.3", parse2?.tokens?.get("12345"))

        parsePaymentRequest("ergo:testaddr&amount=1&token-2345=22.3").apply {
            Assert.assertEquals(Parameters.OneErg, this?.amount?.nanoErgs)
            Assert.assertEquals("testaddr", this?.address)
            Assert.assertEquals(1, this?.tokens?.size)
            Assert.assertEquals("2345", this?.tokens?.keys?.first())
            Assert.assertEquals("22.3", this?.tokens?.get("2345"))
        }
    }
}