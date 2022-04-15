package org.ergoplatform.uilogic.transactions

import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ergoCurrencyText
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.formatFiatToString

data class SuggestedFee(
    val speed: ExecutionSpeed,
    val waitTimeMin: Int,
    val feeAmount: Long
) {
    fun getExecutionSpeedText(stringProvider: StringProvider): String =
        when (speed) {
            ExecutionSpeed.Fast -> stringProvider.getString(STRING_LABEL_FEE_FAST)
            ExecutionSpeed.Medium -> stringProvider.getString(STRING_LABEL_FEE_MEDIUM)
            ExecutionSpeed.Slow -> stringProvider.getString(STRING_LABEL_FEE_SLOW)
        }

    fun getFeeAmountText(stringProvider: StringProvider): String {
        val ergoAmount = ErgoAmount(feeAmount).toStringRoundToDecimals() + " " + ergoCurrencyText

        val walletSyncManager = WalletStateSyncManager.getInstance()
        return if (walletSyncManager.fiatCurrency.isNotEmpty()) {
            "$ergoAmount ≈ " +
                    formatFiatToString(
                        ErgoAmount(feeAmount).toDouble() * walletSyncManager.fiatValue.value,
                        walletSyncManager.fiatCurrency, stringProvider
                    )
        } else ergoAmount
    }

    fun getFeeExecutionTimeText(stringProvider: StringProvider): String =
        stringProvider.getString(
            STRING_LABEL_FEE_EXECUTION_TIME,
            waitTimeMin.toString()
        )
}

enum class ExecutionSpeed { Fast, Medium, Slow }

