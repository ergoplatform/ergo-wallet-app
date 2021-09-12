package org.ergoplatform.android

import org.ergoplatform.appkit.Parameters
import org.junit.Assert.*
import org.junit.Test

class ErgoAmountTest {

    @Test
    fun convertFromString() {
        val feeAmount = ErgoAmount("0.001")
        assertEquals(Parameters.MinFee, feeAmount.nanoErgs)

        val oneErg = ErgoAmount("1")
        assertEquals(Parameters.OneErg, oneErg.nanoErgs)

        val zeroErg = ErgoAmount("")
        assertEquals(0, zeroErg.nanoErgs)

        val floatPrecisionProblem = ErgoAmount("4.503")
        assertEquals(4503000000, floatPrecisionProblem.nanoErgs)

        val tooMuchDecimals = ErgoAmount("1.0000000005")
        assertEquals(Parameters.OneErg, tooMuchDecimals.nanoErgs)
    }

    @Test
    fun convertFromAndToString() {
        val feeAmount = ErgoAmount("0.001")
        assertEquals("0.001000000", feeAmount.toString())

        val floatPrecisionProblem = ErgoAmount("4.503")
        assertEquals("4.503000000", floatPrecisionProblem.toString())
    }

    @Test
    fun math() {
        val minusTest = ErgoAmount(100) - ErgoAmount(1)
        assertEquals(99, minusTest.nanoErgs)
    }


}