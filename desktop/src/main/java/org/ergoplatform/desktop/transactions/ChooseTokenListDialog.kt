package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.ergoplatform.Application
import org.ergoplatform.desktop.tokens.TokenEntryViewData
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_TITLE_ADD_TOKEN

@Composable
fun ChooseTokenListDialog(
    tokenToChooseFrom: List<WalletToken>,
    tokenInfoMap: HashMap<String, TokenInformation>,
    onTokenChosen: (WalletToken) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val preparedList = remember {
        tokenToChooseFrom.map { token ->
            TokenEntryViewData(
                token, false
            ).apply { bind(tokenInfoMap[token.tokenId]) }
        }
    }

    AppDialog(onDismissRequest) {
        Box(Modifier.fillMaxWidth()) {

            val scrollState = rememberScrollState()
            Column(
                Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {
                Text(
                    remember { Application.texts.getString(STRING_TITLE_ADD_TOKEN) },
                    Modifier.fillMaxWidth().padding(defaultPadding),
                    textAlign = TextAlign.Center,
                    style = labelStyle(LabelStyle.BODY1)
                )

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
                            Row(Modifier.align(Alignment.CenterHorizontally)) {
                                // TODO token thumbnail

                                Text(
                                    token.displayedName ?: "",
                                    maxLines = 1,
                                    style = labelStyle(LabelStyle.HEADLINE2),
                                )

                                // TODO token genuity flag
                            }

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
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )

        }
    }
}
