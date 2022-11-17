package org.ergoplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.ergoplatform.ErgoAmount
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.ergoplatform.uilogic.StringProvider

@Composable
fun ErgoAmount.toComposableText(texts: StringProvider, trimTrailingZeros: Boolean = false) = remember(nanoErgs) {
    texts.getString(
        STRING_LABEL_ERG_AMOUNT,
        if (trimTrailingZeros) toStringTrimTrailingZeros()
        else toStringRoundToDecimals()
    )
}

