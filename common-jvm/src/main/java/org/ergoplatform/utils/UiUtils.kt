package org.ergoplatform.utils

import org.ergoplatform.TokenAmount
import org.ergoplatform.uilogic.STRING_FORMAT_FIAT
import org.ergoplatform.uilogic.StringProvider
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

fun inputTextToDouble(amountStr: String?): Double {
    try {
        return if (amountStr.isNullOrEmpty()) 0.0 else amountStr.toDouble()
    } catch (t: Throwable) {
        return 0.0
    }
}

/**
 * fiat is formatted according to users locale, because it is his local currency
 */
fun formatFiatToString(amount: Double, currency: String, text: StringProvider): String {
    return DecimalFormat(text.getString(STRING_FORMAT_FIAT)).format(amount) +
            " " + currency.uppercase(Locale.getDefault())
}

/**
 * Formats token (asset) amounts, always formatted US-style.
 * For larger amounts, 1,120.00 becomes 1.1K, useful for displaying with less space
 */
fun formatTokenAmounts(
    amount: Long,
    decimals: Int,
): String {
    val tokenAmount = TokenAmount(amount, decimals)
    val doubleValue: Double = tokenAmount.toDouble()
    val preciseString = tokenAmount.toString()
    return if (doubleValue < 1000 && preciseString.length < 8 || doubleValue < 1) {
        preciseString
    } else {
        formatDoubleWithPrettyReduction(doubleValue)
    }
}

fun formatDoubleWithPrettyReduction(amount: Double): String {
    val suffixChars = "KMGTPE"
    val formatter = DecimalFormat("###.#", DecimalFormatSymbols(Locale.US))
    formatter.roundingMode = RoundingMode.DOWN

    return if (amount < 1000.0) formatter.format(amount)
    else {
        val exp = (ln(amount) / ln(1000.0)).toInt()
        formatter.format(amount / 1000.0.pow(exp.toDouble())) + suffixChars[exp - 1]
    }
}

fun Throwable.getMessageOrName(): String = message ?: javaClass.name