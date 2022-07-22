package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.PaymentRequest
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.uilogic.STRING_DESC_CHOOSE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_TO
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_TOKEN_BALANCE
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses

@Composable
fun ChooseWalletListDialog(
    wallets: List<Wallet>,
    showTokenNum: Boolean,
    onWalletChosen: (WalletConfig) -> Unit,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    AppDialog(onDismiss, verticalPadding = defaultPadding * 6) {

        Box(Modifier.fillMaxWidth()) {

            val scrollState = rememberScrollState()
            Column(
                Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {
                header?.invoke()

                WalletChooserList(wallets, showTokenNum, onWalletChosen)
            }
            AppScrollbar(scrollState)
        }
    }
}

@Composable
fun PaymentRequestHeader(
    paymentRequest: PaymentRequest
) {
    Column(Modifier.padding(defaultPadding)) {
        Text(
            remember { Application.texts.getString(STRING_BUTTON_SEND) },
            style = labelStyle(LabelStyle.HEADLINE2),
            color = uiErgoColor,
        )

        val amount = paymentRequest.amount

        if (amount.nanoErgs > 0) {
            Text(
                amount.toComposableText(),
                style = labelStyle(LabelStyle.HEADLINE2),
                modifier = Modifier.padding(vertical = defaultPadding / 2)
                    .align(Alignment.CenterHorizontally),
            )
        }

        Text(
            remember { Application.texts.getString(STRING_LABEL_TO) },
            Modifier.align(Alignment.CenterHorizontally),
            style = labelStyle(LabelStyle.BODY1)
        )

        MiddleEllipsisText(
            paymentRequest.address,
            style = labelStyle(LabelStyle.HEADLINE2),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Text(
            remember { Application.texts.getString(STRING_DESC_CHOOSE_WALLET) },
            style = labelStyle(LabelStyle.BODY1),
            modifier = Modifier.padding(vertical = defaultPadding * 1.5f)
        )
    }
}

@Composable
fun WalletChooserList(
    wallets: List<Wallet>,
    showTokenNum: Boolean,
    clickListener: (WalletConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        wallets.forEach { wallet ->
            WalletChooserListItem(
                wallet,
                showTokenNum,
                Modifier.clickable { clickListener(wallet.walletConfig) }
            )
        }
    }
}

@Composable
private fun WalletChooserListItem(
    wallet: Wallet,
    showTokenNum: Boolean,
    modifier: Modifier,
) {
    val tokenNum = if (showTokenNum) wallet.getTokensForAllAddresses().size else 0
    val outerModifier = modifier.padding(defaultPadding)

    val nameAndErgBalance: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(rowModifier.fillMaxWidth()) {
            Text(
                wallet.walletConfig.displayName ?: "",
                maxLines = 1,
                color = uiErgoColor,
                style = labelStyle(LabelStyle.BODY1BOLD),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                ErgoAmount(wallet.getBalanceForAllAddresses()).toComposableText(),
                style = labelStyle(LabelStyle.BODY1BOLD),
                modifier = Modifier.padding(start = defaultPadding / 2),
            )
        }
    }

    if (tokenNum > 0) {
        Column(outerModifier) {
            nameAndErgBalance(Modifier)

            Text(
                remember {
                    Application.texts.getString(
                        STRING_LABEL_WALLET_TOKEN_BALANCE,
                        tokenNum.toString()
                    )
                },
                style = labelStyle(LabelStyle.BODY1BOLD),
                modifier = Modifier.align(Alignment.End),
            )
        }
    } else
        nameAndErgBalance(outerModifier)
}