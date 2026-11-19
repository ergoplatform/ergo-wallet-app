package org.ergoplatform

import org.junit.Assert.assertEquals
import org.junit.Test
import scorex.util.encode.Base16

class ColdClientV3OutputTest {
    // A reduced transaction spending one P2PK box into an output guarded by an ErgoTree v3
    // (sigmaProp(1 + 1 == 2) compiled under block version 4), plus the fee output.
    private val reducedTxHex =
        "a801012d04c563c5d8ed495e5d1f5de675ce0caca08f6d9f48ab3d128272dbe09a20680000000002c082ea0d1b06010101d1730095c4070000c0843d1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730495c4070000cd03f56b14197c1d0f9bf8418ed8c57a3179d12d9af98745fbd0ab3b9dd6883d24a8ad5fad5f"

    @Test
    fun parsesReducedTransactionWithErgoTreeV3Output() {
        val reduced = ErgoFacade.deserializeUnsignedTxOffline(Base16.decode(reducedTxHex).get())
        assertEquals(2, reduced.outputs.size)
    }
}
