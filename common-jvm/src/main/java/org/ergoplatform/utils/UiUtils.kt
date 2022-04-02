package org.ergoplatform.utils

import org.ergoplatform.TokenAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.uilogic.STRING_FORMAT_FIAT
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.ergoplatform.uilogic.StringProvider
import java.math.BigDecimal
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

fun formatTokenPriceToString(
    balanceAmount: TokenAmount,
    pricePerErg: BigDecimal,
    walletSyncManager: WalletStateSyncManager,
    text: StringProvider
): String {
    val ergValue = balanceAmount.toErgoValue(pricePerErg)

    return if (walletSyncManager.fiatCurrency.isNotEmpty()) {
        formatFiatToString(
            ergValue.toDouble() * walletSyncManager.fiatValue.value,
            walletSyncManager.fiatCurrency, text
        )
    } else {
        text.getString(STRING_LABEL_ERG_AMOUNT, ergValue.toStringRoundToDecimals())
    }
}

/**
 * fiat is formatted according to users locale, because it is his local currency
 */
fun formatFiatToString(amount: Double, currency: String, text: StringProvider): String {
    return DecimalFormat(text.getString(STRING_FORMAT_FIAT)).format(amount) +
            " " + currency.uppercase(Locale.getDefault())
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

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun Throwable.getMessageOrName(): String = message ?: javaClass.name