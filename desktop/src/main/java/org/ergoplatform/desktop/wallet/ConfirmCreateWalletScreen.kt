package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun ConfirmCreateWalletScreen(
    onBack: () -> Unit,
    word1Num: Int,
    word2Num: Int,
    errorWords: List<Boolean>,
    onProceed: (word1: String, word2: String, checked: Boolean) -> Unit,
) {
    val word1State = remember { mutableStateOf(TextFieldValue()) }
    val word2State = remember { mutableStateOf(TextFieldValue()) }
    val confirmationState = remember { mutableStateOf(false) }

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
                    Application.texts.getString(STRING_INTRO_CONFIRM_CREATE_WALLET),
                    Modifier.padding(top = defaultPadding),
                    style = labelStyle(LabelStyle.BODY1)
                )

                OutlinedTextField(
                    word1State.value,
                    onValueChange = {
                        word1State.value = it
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding / 2),
                    singleLine = true,
                    isError = errorWords[0],
                    label = {
                        Text(
                            Application.texts.getString(
                                STRING_LABEL_WORD_CONFIRM_CREATE_WALLET, word1Num.toString()
                            )
                        )
                    },
                    colors = appTextFieldColors(),
                )

                OutlinedTextField(
                    word2State.value,
                    onValueChange = {
                        word2State.value = it
                    },
                    Modifier.fillMaxWidth().padding(top = defaultPadding / 2),
                    singleLine = true,
                    isError = errorWords[1],
                    label = {
                        Text(
                            Application.texts.getString(
                                STRING_LABEL_WORD_CONFIRM_CREATE_WALLET, word2Num.toString()
                            )
                        )
                    },
                    colors = appTextFieldColors(),
                )

                Row(Modifier.fillMaxWidth().padding(top = defaultPadding)) {
                    Checkbox(
                        confirmationState.value,
                        onCheckedChange = { confirmationState.value = it },
                        Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        Application.texts.getString(STRING_CHECK_CONFIRM_CREATE_WALLET),
                        Modifier.padding(top = defaultPadding).weight(1f),
                        style = labelStyle(LabelStyle.BODY1)
                    )
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
                        onClick = {
                            onProceed(
                                word1State.value.text,
                                word2State.value.text,
                                confirmationState.value
                            )
                        },
                        colors = primaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_DONE))
                    }
                }
            }
        }
    }
}