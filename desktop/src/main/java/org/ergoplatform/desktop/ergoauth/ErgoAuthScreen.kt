package org.ergoplatform.desktop.ergoauth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.*
import org.ergoplatform.desktop.transactions.ColdSigningResultLayout
import org.ergoplatform.desktop.transactions.ColdSigningScanLayout
import org.ergoplatform.desktop.ui.AppScrollingLayout
import org.ergoplatform.desktop.ui.bigIconSize
import org.ergoplatform.desktop.ui.defaultMaxWidth
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.ergoLogo
import org.ergoplatform.desktop.ui.getSeverityIcon
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.ergoAuthResponseToQrChunks
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic

@Composable
fun ErgoAuthScreen(
    uiLogic: ErgoAuthUiLogic,
    authState: ErgoAuthUiLogic.State,
    scanningState: MutableState<Int>,
    onChangeWallet: () -> Unit,
    onScan: () -> Unit,
    onAuthenticate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppScrollingLayout {

        Column(
            Modifier.widthIn(max = defaultMaxWidth).align(Alignment.Center).padding(defaultPadding)
        ) {
            when (authState) {
                ErgoAuthUiLogic.State.FETCHING_DATA -> {
                    AppProgressIndicator(
                        Modifier.align(Alignment.CenterHorizontally).padding(defaultPadding)
                    )

                    Text(
                        remember { Application.texts.getString(STRING_LABEL_FETCHING_DATA) },
                        Modifier.align(Alignment.CenterHorizontally).padding(defaultPadding),
                        style = labelStyle(LabelStyle.HEADLINE2),
                    )
                }

                ErgoAuthUiLogic.State.SCANNING ->
                    ColdSigningScanLayout(
                        uiLogic.requestPagesCollector!!, Modifier, scanningState,
                        uiLogic.lastMessage, onScan
                    )

                ErgoAuthUiLogic.State.WAIT_FOR_AUTH ->
                    WaitForAuthLayout(uiLogic, onChangeWallet, onAuthenticate)

                ErgoAuthUiLogic.State.DONE ->
                    if (uiLogic.isColdAuth && uiLogic.authResponse != null)
                        ColdSigningResultLayout(
                            uiLogic.authResponse!!, Modifier, onDismiss,
                            ::ergoAuthResponseToQrChunks,
                            descriptionLabel = STRING_DESC_RESPONSE_COLD_AUTH_MULTIPLE,
                            lastPageDescriptionLabel = STRING_DESC_RESPONSE_COLD_AUTH
                        )
                    else
                        AuthDoneLayout(uiLogic, onDismiss)
            }

        }
    }
}

@Composable
private fun AuthDoneLayout(
    uiLogic: ErgoAuthUiLogic,
    onDismiss: () -> Unit
) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(defaultPadding)) {

            uiLogic.getDoneSeverity().getSeverityIcon()?.let { icon ->
                Icon(
                    icon, null,
                    Modifier.padding(bottom = defaultPadding).requiredSize(smallIconSize * 4)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Text(
                uiLogic.getDoneMessage(Application.texts),
                Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
            )

            AppButton(
                onDismiss,
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
            ) {
                Text(remember { Application.texts.getString(STRING_BUTTON_DONE) })
            }

        }
    }
}

@Composable
private fun WaitForAuthLayout(
    uiLogic: ErgoAuthUiLogic,
    onChangeWallet: () -> Unit,
    onAuthenticate: () -> Unit
) {
    AppCard(Modifier.fillMaxWidth().padding(bottom = defaultPadding)) {
        Row(Modifier.padding(defaultPadding)) {
            (uiLogic.ergAuthRequest?.messageSeverity
                ?: MessageSeverity.NONE).getSeverityIcon()?.let { icon ->
                Icon(
                    icon,
                    null,
                    Modifier.padding(end = defaultPadding / 2).size(mediumIconSize)
                        .align(Alignment.CenterVertically)
                )


                Text(
                    remember(uiLogic.ergAuthRequest) {
                        uiLogic.getAuthenticationMessage(Application.texts)
                    },
                    Modifier.align(Alignment.CenterVertically).weight(1f),
                    style = labelStyle(LabelStyle.BODY1),
                )
            }
        }
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(defaultPadding)) {

            Image(
                ergoLogo(),
                null,
                Modifier.align(Alignment.CenterHorizontally).requiredSize(bigIconSize)
            )

            Text(
                remember { Application.texts.getString(STRING_DESC_AUTHENTICATION_WALLET) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                textAlign = TextAlign.Center,
            )

            Row(
                Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onChangeWallet)
            ) {
                Text(
                    uiLogic.walletConfig?.displayName ?: "",
                    style = labelStyle(LabelStyle.HEADLINE2),
                    color = uiErgoColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Icon(
                    Icons.Default.ExpandMore,
                    null,
                    Modifier.align(Alignment.CenterVertically)
                )

            }

            AppButton(
                onAuthenticate,
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
            ) {
                Text(remember { Application.texts.getString(STRING_BUTTON_AUTHENTICATE) })
            }
        }
    }
}