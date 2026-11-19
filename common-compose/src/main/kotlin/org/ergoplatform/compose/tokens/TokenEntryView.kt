package org.ergoplatform.desktop.tokens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.TokenAmount
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.StringProvider

@Composable
fun TokenEntryView(walletToken: WalletToken, texts: StringProvider) {
    val displayName = walletToken.name ?: texts.getString(
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
    val displayNameText: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            text = displayName,
            style = labelStyle(LabelStyle.BODY1),
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (displayAmount.isNotBlank()) {
        Row(modifier) {

            Text(
                displayAmount,
                Modifier.padding(end = defaultPadding / 3),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )

            displayNameText(Modifier.weight(1f, false))
        }
    } else
        displayNameText(modifier)
}