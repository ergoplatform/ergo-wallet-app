package org.ergoplatform

import org.ergoplatform.appkit.Parameters
import org.junit.Assert
import org.junit.Test

class PaymentRequestKtTest {

    @Test
    fun parseContentFromQrCode() {
        val parse1 =
            parseContentFromQrCode("https://explorer.ergoplatform.com/payment-request?address=testaddr&amount=1.0&tokenId=notNumericAmount") { true }

        Assert.assertEquals(Parameters.OneErg, parse1?.amount?.nanoErgs)
        Assert.assertEquals("testaddr", parse1?.address)
        Assert.assertEquals(0, parse1?.tokens?.size)

        val parse2 =
            parseContentFromQrCode("https://explorer.ergoplatform.com/payment-request?address=testaddr&amount=2&12345=22.3") { true }

        Assert.assertEquals(2 * Parameters.OneErg, parse2?.amount?.nanoErgs)
        Assert.assertEquals("testaddr", parse2?.address)
        Assert.assertEquals(1, parse2?.tokens?.size)
        Assert.assertEquals("22.3", parse2?.tokens?.get("12345"))
    }
}