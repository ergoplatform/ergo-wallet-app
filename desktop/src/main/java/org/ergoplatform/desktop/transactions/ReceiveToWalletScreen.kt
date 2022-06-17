package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.getQrCodeImageBitmap
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.wallet.addresses.getAddressLabel

@Composable
fun ReceiveToWalletScreen(walletConfig: WalletConfig, address: WalletAddress) {
    val qrImage = remember(address) { getQrCodeImageBitmap(address.publicAddress) }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()))
    {
        Card(
            Modifier.padding(defaultPadding).align(Alignment.Center).defaultMinSize(400.dp, 200.dp)
                .widthIn(max = 600.dp)
        ) {

            Column(Modifier.padding(defaultPadding)) {

                Text(
                    text = walletConfig.displayName ?: "",
                    color = uiErgoColor,
                    style = labelStyle(LabelStyle.HEADLINE2)
                )

                Text(
                    text = address.getAddressLabel(Application.texts),
                    color = uiErgoColor,
                    style = labelStyle(LabelStyle.BODY1BOLD)
                )

                Image(
                    qrImage,
                    null,
                    Modifier.size(400.dp).padding(defaultPadding)
                        .align(Alignment.CenterHorizontally)
                )

                Row {
                    Text(
                        text = address.publicAddress,
                        style = labelStyle(LabelStyle.HEADLINE2),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )

                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        Modifier.size(24.dp).clickable { // TODO
                        },
                        tint = MosaikStyleConfig.secondaryLabelColor
                    )
                }
            }
        }
    }
}