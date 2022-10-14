package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.toErgoAmount
import org.ergoplatform.uilogic.STRING_BUTTON_APPLY
import org.ergoplatform.uilogic.STRING_LABEL_FEE_CUSTOM
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.uilogic.transactions.SuggestedFee

@Composable
fun ChooseFeeDialog(
    uiLogic: SendFundsUiLogic,
    suggestedFees: List<SuggestedFee>,
    onDismissRequest: () -> Unit,
) {
    AppDialog(onDismissRequest) {
        Box(Modifier.fillMaxWidth()) {

            Column(
                Modifier.fillMaxWidth().padding(defaultPadding)
            ) {

                val ergoApiService = ApiServiceManager.getOrInit(Application.prefs)

                suggestedFees.forEach { suggestedFee ->
                    AppCard(Modifier.padding(bottom = defaultPadding).clickable {
                        uiLogic.setNewFeeAmount(ErgoAmount(suggestedFee.feeAmount), ergoApiService)
                        onDismissRequest()
                    }) {

                        Column(
                            Modifier.padding(defaultPadding).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            key(suggestedFee.feeAmount)
                            {
                                Text(
                                    suggestedFee.getExecutionSpeedText(Application.texts),
                                    style = labelStyle(LabelStyle.BODY1BOLD),
                                    color = uiErgoColor,
                                )
                                Text(
                                    suggestedFee.getFeeAmountText(Application.texts),
                                    style = labelStyle(LabelStyle.BODY1),
                                )
                                Text(
                                    suggestedFee.getFeeExecutionTimeText(Application.texts),
                                    style = labelStyle(LabelStyle.BODY1BOLD),
                                )
                            }

                        }

                    }
                }

                val feeAmountValue =
                    remember { mutableStateOf(TextFieldValue(uiLogic.feeAmount.toStringTrimTrailingZeros())) }

                val apply = {
                    feeAmountValue.value.text.toErgoAmount()?.let {
                        uiLogic.setNewFeeAmount(
                            it,
                            ergoApiService
                        )
                    }
                    onDismissRequest()
                }

                OutlinedTextField(
                    feeAmountValue.value,
                    { textFieldValue ->
                        feeAmountValue.value = textFieldValue
                    },
                    Modifier.padding(bottom = org.ergoplatform.compose.settings.defaultPadding)
                        .fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { apply() }),
                    maxLines = 1,
                    singleLine = true,
                    label = { Text(remember { Application.texts.getString(STRING_LABEL_FEE_CUSTOM) }) },
                    colors = appTextFieldColors(),
                )

                AppButton(
                    onClick = apply,
                    colors = primaryButtonColors(),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(remember { Application.texts.getString(STRING_BUTTON_APPLY) })
                }
            }
        }
    }
}
