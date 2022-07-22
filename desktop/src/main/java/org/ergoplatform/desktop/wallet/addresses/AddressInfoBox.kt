package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.toComposableText
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_TOKEN_BALANCE
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.addresses.isDerivedAddress
import org.ergoplatform.wallet.getStateForAddress
import org.ergoplatform.wallet.getTokensForAddress


@Composable
fun AddressInfoBox(walletAddress: WalletAddress, wallet: Wallet?, showFullInfo: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.padding(defaultPadding)) {
        val addressLabel = walletAddress.getAddressLabel(Application.texts)
        if (showFullInfo)
            Row {
                if (walletAddress.isDerivedAddress()) {
                    Text(
                        walletAddress.derivationIndex.toString(),
                        Modifier.align(Alignment.CenterVertically).padding(end = defaultPadding),
                        style = labelStyle(LabelStyle.HEADLINE1),
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        addressLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = labelStyle(LabelStyle.BODY1BOLD),
                        color = uiErgoColor,
                    )
                    MiddleEllipsisText(walletAddress.publicAddress)
                }
            }
        else
            Text(
                addressLabel,
                Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = uiErgoColor,
            )

        if (wallet != null) {
            if (showFullInfo)
                Divider(
                    Modifier.padding(vertical = defaultPadding / 2),
                    color = MosaikStyleConfig.secondaryLabelColor
                )

            val state = wallet.getStateForAddress(walletAddress.publicAddress)
            val tokens = wallet.getTokensForAddress(walletAddress.publicAddress)

            Row(
                (if (!showFullInfo) Modifier.padding(top = defaultPadding / 2) else Modifier)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = ErgoAmount(state?.balance ?: 0).toComposableText(),
                    style = labelStyle(LabelStyle.BODY1BOLD)
                )
                if (tokens.isNotEmpty()) {
                    Text(
                        Application.texts.getString(
                            STRING_LABEL_WALLET_TOKEN_BALANCE,
                            tokens.size.toString()
                        ),
                        Modifier.padding(start = defaultPadding * 1.5f),
                        style = labelStyle(LabelStyle.BODY1BOLD)
                    )
                }
            }
        }
    }
}