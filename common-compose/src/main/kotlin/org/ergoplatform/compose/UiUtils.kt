package org.ergoplatform.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.ergoplatform.ErgoAmount
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.ergoplatform.uilogic.StringProvider

/**
 * @return a remembered String value with currency label ("x ERG")
 * @param trimTrailingZeros if set to true, the returned erg value will be precise but trailing zeroes
 * trimmed. if set to false, returned erg value will be rounded to default rounding value.
 */
@Composable
fun ErgoAmount.toComposableText(texts: StringProvider, trimTrailingZeros: Boolean = false) = remember(nanoErgs) {
    texts.getString(
        STRING_LABEL_ERG_AMOUNT,
        if (trimTrailingZeros) toStringTrimTrailingZeros()
        else toStringRoundToDecimals()
    )
}

