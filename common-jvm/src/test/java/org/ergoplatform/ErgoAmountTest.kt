package org.ergoplatform

import org.ergoplatform.appkit.Parameters
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class ErgoAmountTest {

    @Test
    fun convertFromString() {
        val feeAmount = ErgoAmount("0.001")
        Assert.assertEquals(Parameters.MinFee, feeAmount.nanoErgs)

        val oneErg = ErgoAmount("1")
        Assert.assertEquals(Parameters.OneErg, oneErg.nanoErgs)
        Assert.assertEquals(BigDecimal.ONE, oneErg.toBigDecimal().stripTrailingZeros())

        val zeroErg = ErgoAmount("")
        Assert.assertEquals(0, zeroErg.nanoErgs)
        Assert.assertEquals(BigDecimal.ZERO, zeroErg.toBigDecimal().stripTrailingZeros())

        val floatPrecisionProblem = ErgoAmount("4.503")
        Assert.assertEquals(4503000000, floatPrecisionProblem.nanoErgs)

        val tooMuchDecimals = "1.0000000005".toErgoAmount()
        Assert.assertEquals(null, tooMuchDecimals?.nanoErgs)

        val maxLongAmount = (Long.MAX_VALUE / Parameters.OneErg).toString().toErgoAmount()
        Assert.assertEquals(
            (Long.MAX_VALUE / Parameters.OneErg) * Parameters.OneErg,
            maxLongAmount?.nanoErgs
        )

        val bigNumber = "10000000000".toErgoAmount()
        Assert.assertEquals(null, bigNumber?.nanoErgs)
    }

    @Test
    fun convertFromAndToString() {
        val feeAmount = ErgoAmount("0.001")
        Assert.assertEquals("0.001000000", feeAmount.toString())
        Assert.assertEquals("0.001", feeAmount.toStringTrimTrailingZeros())

        val oneErg = ErgoAmount("001")
        Assert.assertEquals("1.000000000", oneErg.toString())
        Assert.assertEquals("1", oneErg.toStringTrimTrailingZeros())

        val floatPrecisionProblem = ErgoAmount("4.503")
        Assert.assertEquals("4.503000000", floatPrecisionProblem.toString())
        Assert.assertEquals("4.503", floatPrecisionProblem.toStringTrimTrailingZeros())
    }

    @Test
    fun math() {
        val minusTest = ErgoAmount(100) - ErgoAmount(1)
        Assert.assertEquals(99, minusTest.nanoErgs)
        Assert.assertEquals(100, (ErgoAmount(1) + minusTest).nanoErgs)
    }


}