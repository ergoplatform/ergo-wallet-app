package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.TokenAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.toTokenAmount
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.uilogic.STRING_LABEL_FIAT_AMOUNT
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.utils.formatTokenPriceToString

@Composable
fun SendTokenItem(
    token: WalletToken,
    tokensError: MutableState<Boolean>,
    uiLogic: SendFundsUiLogic,
    onRemove: () -> Unit,
) {

    val tokenId = token.tokenId!!
    val amountChosen = uiLogic.tokensChosen[tokenId]!!.value
    val tokenPrice =
        remember(tokenId) { WalletStateSyncManager.getInstance().getTokenPrice(tokenId) }
    val inputAmount = remember(tokenId) {
        mutableStateOf(
            TextFieldValue(
                uiLogic.tokenAmountToText(amountChosen, token.decimals)
            )
        )
    }
    val tokenPriceState = remember(inputAmount.value, tokenPrice) {
        tokenPrice?.let {
            mutableStateOf(
                Application.texts.getString(
                    STRING_LABEL_FIAT_AMOUNT, formatTokenPriceToString(
                        inputAmount.value.text.toTokenAmount(token.decimals)
                            ?: TokenAmount(0, token.decimals),
                        it.ergValue,
                        WalletStateSyncManager.getInstance(),
                        Application.texts,
                    )
                )
            )
        }
    }
    val isSingular =
        token.isSingularToken() && amountChosen == 1L && tokenPrice == null

    Box(Modifier.padding(vertical = defaultPadding / 4)) {
        Box(
            Modifier.border(1.dp, MosaikStyleConfig.defaultLabelColor, RoundedCornerShape(8.dp))
                .padding(defaultPadding / 2)
        ) {
            Row {

                Column(Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically)) {

                    Row(Modifier.fillMaxWidth()) {

                        Text(
                            token.name
                                ?: Application.texts.getString(STRING_LABEL_UNNAMED_TOKEN),
                            Modifier.weight(.55f).align(Alignment.CenterVertically),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = labelStyle(LabelStyle.BODY1),
                        )

                        if (!isSingular) {
                            TextField(
                                inputAmount.value,
                                onValueChange = {
                                    inputAmount.value = it
                                    uiLogic.setTokenAmount(
                                        token.tokenId!!,
                                        it.text.toTokenAmount(token.decimals) ?: TokenAmount(
                                            0,
                                            token.decimals
                                        )
                                    )
                                    tokensError.value = false
                                },
                                singleLine = true,
                                modifier = Modifier.weight(.45f).scale(.8f)
                            )
                        }
                    }

                    if (!isSingular) {
                        Row(Modifier.fillMaxWidth()) {
                            Row(Modifier.clickable {
                                uiLogic.setTokenAmount(token.tokenId!!, token.toTokenAmount())
                                inputAmount.value = TextFieldValue(
                                    uiLogic.tokenAmountToText(
                                        token.amount ?: 0,
                                        token.decimals
                                    )
                                )
                            }) {

                                Icon(
                                    Icons.Default.ArrowCircleDown,
                                    null,
                                    Modifier.size(defaultPadding * 1.5f),
                                    tint = MosaikStyleConfig.defaultLabelColor
                                )
                                Text(
                                    token.toTokenAmount().toStringPrettified(),
                                    Modifier.padding(start = defaultPadding / 4),
                                    style = labelStyle(LabelStyle.BODY1BOLD),
                                    maxLines = 1,
                                )
                            }

                            if (tokenPriceState != null)
                                Text(
                                    tokenPriceState.value,
                                    Modifier.weight(1f).padding(
                                        start = defaultPadding,
                                        end = defaultPadding * 1.5f
                                    ),
                                    style = labelStyle(LabelStyle.BODY1),
                                    color = MosaikStyleConfig.secondaryLabelColor,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                )
                        }
                    }

                }

                IconButton(onRemove, Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Default.Close, null, tint = MosaikStyleConfig.secondaryLabelColor)
                }
            }
        }
    }
}