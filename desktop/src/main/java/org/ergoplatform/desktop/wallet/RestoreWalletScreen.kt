package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_BUTTON_RESTORE
import org.ergoplatform.uilogic.STRING_INTRO_RESTORE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_RESTORE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_RESTORE_WALLET_WORD_LIST

@Composable
fun RestoreWalletScreen(
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onTextChange: () -> Unit,
    mnemonicTextState: MutableState<TextFieldValue>,
    hintText: String,
) {
    AppScrollingLayout {
        AppCard(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {
                Text(
                    Application.texts.getString(STRING_LABEL_RESTORE_WALLET),
                    style = labelStyle(LabelStyle.HEADLINE2),
                    color = uiErgoColor
                )

                Text(
                    Application.texts.getString(STRING_INTRO_RESTORE_WALLET),
                    Modifier.padding(top = defaultPadding, bottom = defaultPadding / 2),
                    style = labelStyle(LabelStyle.BODY1)
                )

                LinkifyText(
                    Application.texts.getString(STRING_LABEL_RESTORE_WALLET_WORD_LIST),
                    Modifier.padding(bottom = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1),
                    isHtml = true,
                )

                OutlinedTextField(
                    mnemonicTextState.value,
                    onValueChange = {
                        val textChanged = it.text != mnemonicTextState.value.text
                        mnemonicTextState.value = it
                        if (textChanged)
                            onTextChange()
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding).height(200.dp),
                    colors = appTextFieldColors(),
                    isError = hintText.isNotEmpty(),
                )

                if (hintText.isNotEmpty())
                    Text(hintText, color = uiErgoColor, style = labelStyle(LabelStyle.BODY2))

                Row(Modifier.align(Alignment.End).padding(top = defaultPadding)) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(end = defaultPadding),
                        colors = secondaryButtonColors()
                    ) {
                        Text("Back") // TODO i18n
                    }

                    Button(
                        onClick = onProceed,
                        colors = primaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_RESTORE))
                    }
                }
            }
        }
    }
}