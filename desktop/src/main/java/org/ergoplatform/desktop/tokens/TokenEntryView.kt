package org.ergoplatform.desktop.tokens

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    TokenEntryView(amount, displayName)
}

@Composable
fun TokenEntryView(displayAmount: String, displayName: String, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(if (displayAmount.isNotBlank()) "$displayAmount " else "")
            pop()
            append(displayName)
            toAnnotatedString()
        },
        style = labelStyle(LabelStyle.BODY1),
        modifier = modifier,
    )
}