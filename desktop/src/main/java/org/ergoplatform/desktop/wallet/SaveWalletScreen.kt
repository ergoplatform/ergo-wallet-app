package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun SaveWalletScreen(
    newAddress: String,
    altAddressAvailable: Boolean,
    derivedAddressNum: Int,
    walletName: MutableState<TextFieldValue?>,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onUseAltAddress: () -> Unit,
) {
    AppScrollingLayout {
        AppCard(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {
                Text(
                    Application.texts.getString(STRING_INTRO_SAVE_WALLET),
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = labelStyle(LabelStyle.BODY1)
                )

                Row(Modifier.padding(defaultPadding)) {
                    Image(
                        ergoLogo(),
                        null,
                        colorFilter = ColorFilter.tint(MosaikStyleConfig.secondaryButtonTextColor),
                        modifier = Modifier.height(90.dp).align(Alignment.CenterVertically)
                            .padding(end = defaultPadding)
                    )

                    Text(
                        newAddress,
                        Modifier.align(Alignment.CenterVertically),
                        style = labelStyle(LabelStyle.HEADLINE2),
                    )
                }

                if (altAddressAvailable)
                    Text(
                        newAddress,
                        Modifier.padding(defaultPadding / 2).fillMaxWidth().clickable { onUseAltAddress() },
                        textAlign = TextAlign.Center,
                        style = labelStyle(LabelStyle.BODY1BOLD),
                        color = uiErgoColor,
                    )

                Text(
                    if (derivedAddressNum > 0) {
                        Application.texts.getString(
                            STRING_INTRO_SAVE_WALLET_DERIVED_ADDRESSES_NUM,
                            (derivedAddressNum + 1).toString()
                        )
                    } else
                        Application.texts.getString(STRING_INTRO_SAVE_WALLET2),
                    Modifier.padding(top = defaultPadding / 2, bottom = defaultPadding * 1.5f).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = labelStyle(LabelStyle.BODY1),
                )

                val walletNameValue = walletName.value
                if (walletNameValue != null)
                    OutlinedTextField(
                        walletNameValue,
                        onValueChange = {
                            walletName.value = it
                        },
                        Modifier.fillMaxWidth().padding(bottom = defaultPadding * 1.5f),
                        maxLines = 1,
                        singleLine = true,
                        label = { Text(Application.texts.getString(STRING_LABEL_WALLET_NAME)) },
                        colors = appTextFieldColors(),
                    )

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
                        Text(Application.texts.getString(STRING_BUTTON_SAVE_PASSWORD_ENCRYPTED))
                    }
                }
            }
        }
    }
}