package org.ergoplatform.desktop.tokens

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import org.ergoplatform.Application
import org.ergoplatform.TokenAmount
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN

@Composable
fun TokenEntryView(walletToken: WalletToken) {
    val displayName = walletToken.name ?: Application.texts.getString(
        STRING_LABEL_UNNAMED_TOKEN
    )
    val amount = TokenAmount(
        walletToken.amount ?: 0,
        walletToken.decimals,
    ).toStringPrettified()
    val tokenVal = if (amount.isNotBlank()) "$amount " else ""

    TokenEntryView(tokenVal, displayName)
}

@Composable
fun TokenEntryView(tokenVal: String, displayName: String) {
    Text(
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(tokenVal)
            pop()
            append(displayName)
            toAnnotatedString()
        },
        style = labelStyle(LabelStyle.BODY1),
    )
}