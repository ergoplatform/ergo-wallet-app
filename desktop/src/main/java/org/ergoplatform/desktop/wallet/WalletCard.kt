package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.tokens.TokenEntryView
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.tokens.fillTokenOverview
import org.ergoplatform.tokens.getTokenErgoValueSum
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.utils.formatTokenPriceToString
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses
import org.ergoplatform.wallet.getUnconfirmedBalanceForAllAddresses
import org.ergoplatform.wallet.isReadOnly

@Composable
fun WalletCard(
    wallet: Wallet,
    fiatValue: Float,
    onSendClicked: (WalletConfig) -> Unit,
    onReceiveClicked: (WalletConfig) -> Unit,
    onDetailsClicked: (WalletConfig) -> Unit,
    onSettingsClicked: (WalletConfig) -> Unit,
) {
    AppCard(
        modifier = Modifier.padding(defaultPadding).defaultMinSize(400.dp)
            .widthIn(max = defaultMaxWidth),
    ) {
        Box {
            IconButton(
                { onSettingsClicked(wallet.walletConfig) },
                Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Settings,
                    null,
                )
            }

            Column(Modifier.padding(defaultPadding)) {
                Row {

                    Icon(
                        if (wallet.isReadOnly()) Icons.Default.ZoomIn else Icons.Default.AccountBalanceWallet,
                        null,
                        Modifier.align(Alignment.Top).size(bigIconSize)
                    )

                    Column(
                        modifier = Modifier.align(Alignment.Top).padding(start = defaultPadding),
                        verticalArrangement = Arrangement.Center,
                    ) {

                        Text(
                            text = wallet.walletConfig.displayName!!,
                            color = uiErgoColor,
                            style = labelStyle(LabelStyle.BODY1BOLD)
                        )

                        val balanceErgoAmount = ErgoAmount(wallet.getBalanceForAllAddresses())
                        val unconfirmed = wallet.getUnconfirmedBalanceForAllAddresses() != 0L

                        Text(
                            text = balanceErgoAmount.toComposableText()
                                    + (if (unconfirmed) "*" else ""),
                            style = labelStyle(LabelStyle.HEADLINE1)
                        )

                        val walletSyncManager = WalletStateSyncManager.getInstance()
                        if (walletSyncManager.hasFiatValue) {

                            Text(
                                text = formatFiatToString(
                                    balanceErgoAmount.toDouble() * fiatValue,
                                    walletSyncManager.fiatCurrency, Application.texts
                                ),
                                color = MosaikStyleConfig.secondaryLabelColor,
                                style = labelStyle(LabelStyle.BODY1)
                            )


                        }


                        val tokens = wallet.getTokensForAllAddresses()
                        val tokenCount = tokens.size

                        if (tokenCount > 0) {

                            val tokenValueToShow = getTokenErgoValueSum(tokens, walletSyncManager)


                            Row(Modifier.padding(top = defaultPadding)) {
                                Text(
                                    text = Application.texts.getString(
                                        STRING_LABEL_WALLET_TOKEN_BALANCE,
                                        tokenCount
                                    ),
                                    style = labelStyle(LabelStyle.HEADLINE2),
                                )

                                if (!tokenValueToShow.isZero()) {
                                    Text(
                                        text = formatTokenPriceToString(
                                            tokenValueToShow,
                                            walletSyncManager,
                                            Application.texts
                                        ),
                                        style = labelStyle(LabelStyle.BODY1),
                                        color = MosaikStyleConfig.secondaryLabelColor,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                            .padding(start = defaultPadding / 2),
                                    )
                                }
                            }

                            val tokenList = mutableListOf<WalletToken>()
                            var moreTokenHint: Int? = null
                            fillTokenOverview(
                                tokens,
                                addToken = { tokenList.add(it) },
                                addMoreTokenHint = { moreTokenHint = it }
                            )

                            tokenList.forEach { walletToken ->
                                TokenEntryView(walletToken, Application.texts)
                            }

                            moreTokenHint?.let {
                                TokenEntryView(
                                    "+$moreTokenHint ",
                                    Application.texts.getString(STRING_LABEL_MORE_TOKENS)
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { onDetailsClicked(wallet.walletConfig) },
                    Modifier.fillMaxWidth().padding(top = defaultPadding),
                    border = BorderStroke(
                        ButtonDefaults.OutlinedBorderSize, MosaikStyleConfig.secondaryButtonColor
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MosaikStyleConfig.secondaryButtonColor)
                ) {
                    Text(Application.texts.getString(STRING_LABEL_DETAILS))
                }

                Row(Modifier) {
                    Button(
                        onClick = { onReceiveClicked(wallet.walletConfig) },
                        modifier = Modifier.weight(1f),
                        colors = secondaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_RECEIVE))
                    }
                    Box(Modifier.size(defaultPadding))
                    Button(
                        onClick = { onSendClicked(wallet.walletConfig) },
                        modifier = Modifier.weight(1f),
                        colors = primaryButtonColors()
                    ) {
                        Text(Application.texts.getString(STRING_BUTTON_SEND))
                    }
                }
            }
        }
    }

}