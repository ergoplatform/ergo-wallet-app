package org.ergoplatform

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

class TokenAmount(val rawValue: Long, val decimals: Int) {

    constructor(tokenString: String, decimals: Int) : this(
        if (tokenString.isBlank()) 0
        else tokenString.toBigDecimal().movePointRight(decimals).longValueExact(), decimals
    )

    /**
     * formats with full amount of decimals and without thousands separator
     */
    override fun toString(): String {
        return toBigDecimal().toPlainString()
    }

    /**
     * formats with needed amount of decimals and without thousands separator
     */
    fun toStringTrimTrailingZeros(): String {
        val stringWithTrailingZeros = toString()
        if (decimals > 0) {
            return stringWithTrailingZeros.trimEnd('0').trimEnd('.')
        } else {
            return stringWithTrailingZeros
        }
    }

    /**
     * formats with thousands separators
     */
    fun toStringUsFormatted(trimTrailingZeros: Boolean = true): String {
        val numberInstance = NumberFormat.getNumberInstance(Locale.US)
        numberInstance.maximumFractionDigits = decimals
        if (!trimTrailingZeros)
            numberInstance.minimumFractionDigits = decimals
        return numberInstance.format(toBigDecimal())
    }

    fun toBigDecimal() = rawValue.toBigDecimal().movePointLeft(decimals)

    fun toDouble(): Double {
        return (rawValue.toDouble()) / (10.0.pow(decimals))
    }

    fun toErgoValue(pricePerErg: BigDecimal): ErgoAmount {
        return ErgoAmount(toBigDecimal().divide(pricePerErg, nanoPowerOfTen, RoundingMode.HALF_UP))
    }
}

fun String.toTokenAmount(decimals: Int): TokenAmount? {
    try {
        return TokenAmount(this, decimals)
    } catch (t: Throwable) {
        return null
    }
}