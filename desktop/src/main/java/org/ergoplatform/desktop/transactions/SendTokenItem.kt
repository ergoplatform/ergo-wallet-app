package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.TextFieldValue
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
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic

@Composable
fun SendTokenItem(
    token: WalletToken,
    tokensError: MutableState<Boolean>,
    uiLogic: SendFundsUiLogic,
    onRemove: () -> Unit,
) {

    val amountChosen = uiLogic.tokensChosen[token.tokenId!!]!!.value
    val tokenPrice = WalletStateSyncManager.getInstance().getTokenPrice(token.tokenId)
    val isSingular =
        token.isSingularToken() && amountChosen == 1L && tokenPrice == null

    Box(Modifier.padding(vertical = defaultPadding / 4)) {
        Box(
            Modifier.border(1.dp, MosaikStyleConfig.defaultLabelColor, RoundedCornerShape(8.dp))
                .padding(defaultPadding / 2)
        ) {
            Row {

                Row(Modifier.fillMaxWidth().weight(1f).align(Alignment.CenterVertically)) {

                    Text(
                        token.name
                            ?: Application.texts.getString(STRING_LABEL_UNNAMED_TOKEN),
                        Modifier.weight(.55f).align(Alignment.CenterVertically),
                        maxLines = 1,
                        style = labelStyle(LabelStyle.BODY1),
                    )

                    if (!isSingular) {
                        val inputAmount = remember {
                            mutableStateOf(
                                TextFieldValue(
                                    uiLogic.tokenAmountToText(amountChosen, token.decimals)
                                )
                            )
                        }
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
                                // TODO update token price
                                tokensError.value = false
                            },
                            singleLine = true,
                            modifier = Modifier.weight(.45f).scale(.8f)
                        )
                    }
                }

                IconButton(onRemove, Modifier.align(Alignment.CenterVertically)) {
                    Icon(Icons.Default.Close, null, tint = MosaikStyleConfig.secondaryLabelColor)
                }
            }
        }
    }

    // TODO tokens show price, show balance with max amount setter
//    if (isSingular) {
//        itemBinding.labelTokenBalance.visibility = View.GONE
//        itemBinding.labelBalanceValue.visibility = View.GONE
//    } else {
//          tokenDbEntity.toTokenAmount().toStringPrettified() max Amount
//        itemBinding.inputTokenAmount.addTextChangedListener(
//            TokenAmountWatcher(
//                tokenDbEntity,
//                tokenPrice,
//                itemBinding
//            )
//        )
//        itemBinding.labelBalanceValue.visibility =
//            if (tokenPrice == null) View.GONE else View.VISIBLE
//        itemBinding.inputTokenAmount.setText(
//            viewModel.uiLogic.tokenAmountToText(
//                amountChosen,
//                tokenDbEntity.decimals
//            )
//        )
//        itemBinding.labelTokenBalance.setOnClickListener {
//            itemBinding.inputTokenAmount.setText(
//                viewModel.uiLogic.tokenAmountToText(
//                    tokenDbEntity.amount!!,
//                    tokenDbEntity.decimals
//                )
//            )
//        }
//    }
}