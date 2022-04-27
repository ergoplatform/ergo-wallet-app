package org.ergoplatform.uilogic

import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.nanoPowerOfTen
import org.ergoplatform.toErgoAmount
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * class will automatically convert between fiat and ergo value, if possible
 */
class ErgoOrFiatAmount {
    var inputIsFiat: Boolean = false
        private set
    var ergAmount: ErgoAmount = ErgoAmount.ZERO
        private set
    var fiatAmount: BigDecimal = BigDecimal.ZERO
        private set
    private val fiatValue get() = WalletStateSyncManager.getInstance().fiatValue.value

    fun inputAmountChanged(input: String) {
        if (!inputIsFiat) {
            setErgAmount(input.toErgoAmount() ?: ErgoAmount.ZERO)
        } else {
            val fiatAmount = try {
                input.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
            } catch (t: Throwable) {
                BigDecimal.ZERO
            }
            setFiatAmount(fiatAmount)
        }
    }

    fun setErgAmount(erg: ErgoAmount) {
        ergAmount = erg
        fiatAmount =
            BigDecimal(ergAmount.toDouble() * fiatValue).setScale(2, RoundingMode.HALF_UP)
    }

    fun setFiatAmount(fiatAmount: BigDecimal) {
        this.fiatAmount = fiatAmount
        ergAmount = try {
            ErgoAmount(
                fiatAmount.divide(
                    fiatValue.toBigDecimal(),
                    nanoPowerOfTen,
                    RoundingMode.HALF_UP
                )
            )
        } catch (t: Throwable) {
            ErgoAmount.ZERO
        }
    }

    fun switchInputAmountMode(): Boolean {
        val canChangeMode = WalletStateSyncManager.getInstance().fiatCurrency.isNotEmpty()
        inputIsFiat = if (canChangeMode)
            !inputIsFiat
        else
            false
        return canChangeMode
    }

    fun getInputAmountString(): String {
        return when {
            ergAmount.isZero() -> ""
            inputIsFiat -> fiatAmount.toString()
            else -> ergAmount.toStringTrimTrailingZeros()
        }
    }
}