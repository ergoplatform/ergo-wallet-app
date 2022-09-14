package org.ergoplatform.desktop.tokens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.compose.tokens.TokenLabel
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.getExplorerTokenUrl
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.foregroundColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic

@Composable
fun TokenInformationScreen(
    tokenInformation: TokenInformationComponent.DesktopTokenInformationLayoutLogic?,
    infoLoading: Boolean,
    downloadState: TokenInformationModelLogic.StateDownload,
) {
    AppScrollingLayout {
        if (infoLoading) {
            AppProgressIndicator()

        } else if (tokenInformation == null) {
            Text(
                remember { Application.texts.getString(STRING_LABEL_ERROR_FETCHING) },
                Modifier.align(Alignment.Center).padding(horizontal = defaultPadding)
            )

        } else {

            TokenInfoLayout(
                tokenInformation,
                downloadState,
            )

        }
    }
}

@Composable
private fun BoxScope.TokenInfoLayout(
    tokenLayoutLogic: TokenInformationComponent.DesktopTokenInformationLayoutLogic,
    downloadState: TokenInformationModelLogic.StateDownload,
) {
    Column(
        Modifier.widthIn(max = defaultMaxWidth).align(Alignment.Center).padding(defaultPadding)
    ) {

        TokenLabel(
            tokenLayoutLogic.tokenInformation!!, amountText = null,
            Modifier.align(Alignment.CenterHorizontally),
            showTumbnail = false
        )

        MiddleEllipsisText(
            tokenLayoutLogic.tokenId!!,
            Modifier.align(Alignment.CenterHorizontally).padding(vertical = defaultPadding / 2)
                .clickable { openBrowser(getExplorerTokenUrl(tokenLayoutLogic.tokenId!!)) },
        )

        tokenLayoutLogic.balanceAmount?.let { balanceAmount ->
            Text(
                remember { Application.texts.getString(STRING_TITLE_WALLET_BALANCE) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )

            Text(balanceAmount, Modifier.align(Alignment.CenterHorizontally))

            tokenLayoutLogic.balanceValue?.let {
                Text(
                    it,
                    Modifier.align(Alignment.CenterHorizontally),
                    color = foregroundColor(ForegroundColor.SECONDARY)
                )
            }
        }

        tokenLayoutLogic.supplyAmount?.let {
            Text(
                remember { Application.texts.getString(STRING_LABEL_TOKEN_SUPPLY) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )
            Text(it, Modifier.align(Alignment.CenterHorizontally))
        }

        tokenLayoutLogic.description?.let {
            Text(
                remember { Application.texts.getString(STRING_LABEL_TOKEN_DESCRIPTION) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )
            Text(it, Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        }

        Text(
            remember { Application.texts.getString(STRING_LABEL_MINTING_TX) },
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding).clickable {
                openBrowser(getExplorerTxUrl(tokenLayoutLogic.tokenInformation!!.mintingTxId))
            },
            style = labelStyle(LabelStyle.BODY1LINK),
            color = uiErgoColor,
        )
    }
}
