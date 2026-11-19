package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.ergoplatform.Application
import org.ergoplatform.compose.tokens.TokenEntryViewData
import org.ergoplatform.compose.tokens.TokenLabel
import org.ergoplatform.desktop.tokens.TokenFilterMenu
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.AppScrollbar
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_TITLE_ADD_TOKEN
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic

@Composable
fun ChooseTokenListDialog(
    uiLogic: SendFundsUiLogic,
    refreshCount: Int,
    onDismissRequest: () -> Unit,
) {
    val tokenInfoMap = uiLogic.tokensInfo
    val onTokenChosen: (WalletToken) -> Unit = { uiLogic.newTokenChosen(it.tokenId!!) }

    val preparedList = remember(refreshCount) {
        uiLogic.getTokensToChooseFrom().map { token ->
            TokenEntryViewData(
                token, false, Application.texts
            ).apply { bind(tokenInfoMap[token.tokenId]) }
        }
    }

    AppDialog(onDismissRequest) {
        Box(Modifier.fillMaxWidth()) {

            val scrollState = rememberScrollState()
            Column(
                Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(defaultPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        remember { Application.texts.getString(STRING_TITLE_ADD_TOKEN) },
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = labelStyle(LabelStyle.BODY1)
                    )

                    TokenFilterMenu(uiLogic)
                }

                preparedList.forEach { token ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTokenChosen(token.walletToken)
                                onDismissRequest()
                            }.padding(
                                horizontal = defaultPadding,
                                vertical = defaultPadding / 2
                            )
                    ) {

                        Column(Modifier.fillMaxWidth()) {
                            TokenLabel(
                                token,
                                Modifier.align(Alignment.CenterHorizontally),
                                showAmount = false
                            )

                            if (token.displayedId != null)
                                MiddleEllipsisText(
                                    token.displayedId ?: "",
                                    color = MosaikStyleConfig.secondaryLabelColor,
                                    style = labelStyle(LabelStyle.BODY2)
                                )
                        }

                    }
                }
            }
            AppScrollbar(scrollState)
        }
    }
}
