package org.ergoplatform

import junit.framework.TestCase
import org.junit.Assert

class TokenAmountTest : TestCase() {

    fun testTestToString() {

        Assert.assertEquals("1", TokenAmount(1, 0).toStringTrimTrailingZeros())
        Assert.assertEquals("10", TokenAmount(100, 1).toStringTrimTrailingZeros())
        Assert.assertEquals("10.0", TokenAmount(100, 1).toString())
        Assert.assertEquals("10.5", TokenAmount(1050, 2).toStringTrimTrailingZeros())
        Assert.assertEquals("10", TokenAmount(1000, 2).toStringTrimTrailingZeros())
        Assert.assertEquals("0", TokenAmount(0, 2).toStringTrimTrailingZeros())
    }
}