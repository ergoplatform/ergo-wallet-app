package org.ergoplatform.android

import org.junit.Test

import org.junit.Assert.*

class QRUtilsKtTest {

    @Test
    fun parseContentFromQrCode() {
        val parse1 =
            parseContentFromQrCode("https://explorer.ergoplatform.com/payment-request?address=testaddr&amount=1.0&tokenId=notNumericAmount")

        assertEquals(1f, parse1?.amount)
        assertEquals("testaddr", parse1?.address)
        assertEquals(0, parse1?.tokens?.size)

        val parse2 =
            parseContentFromQrCode("https://explorer.ergoplatform.com/payment-request?address=testaddr&amount=2&12345=22.3")

        assertEquals(2f, parse2?.amount)
        assertEquals("testaddr", parse2?.address)
        assertEquals(1, parse2?.tokens?.size)
        assertEquals(22.3, parse2?.tokens?.get("12345"))
    }
}