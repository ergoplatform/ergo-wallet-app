package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun CreateWalletScreen(
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onCopy: () -> Unit,
    mnemonic: String,
) {
    AppScrollingLayout {
        AppCard(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {
                Text(
                    Application.texts.getString(STRING_LABEL_CREATE_WALLET),
                    style = labelStyle(LabelStyle.HEADLINE2),
                    color = uiErgoColor
                )

                Text(
                    Application.texts.getString(STRING_INTRO_CREATE_WALLET),
                    Modifier.padding(top = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1)
                )

                Row(
                    Modifier.padding(
                        top = defaultPadding,
                        bottom = defaultPadding,
                        start = defaultPadding
                    )
                ) {
                    Text(
                        mnemonic,
                        Modifier.weight(1f),
                        style = labelStyle(LabelStyle.HEADLINE2),
                    )
                    IconButton(
                        onClick = { onCopy() },
                        Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                    }
                }

                Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(end = defaultPadding),
                        colors = secondaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_BACK))
                    }

                    Button(
                        onClick = onProceed,
                        colors = primaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_DONE))
                    }
                }
            }
        }
    }
}