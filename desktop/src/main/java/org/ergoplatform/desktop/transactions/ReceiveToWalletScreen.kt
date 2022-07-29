package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressButton
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_LABEL_COPIED

@Composable
fun ReceiveToWalletScreen(
    walletConfig: WalletConfig,
    address: WalletAddress,
    onChooseAddress: () -> Unit,
    scaffoldState: ScaffoldState?
) {
    val qrImage = remember(address) { getQrCodeImageBitmap(address.publicAddress) }
    val coroutineScope = rememberCoroutineScope()

    AppScrollingLayout {
        Card(
            Modifier.padding(defaultPadding).align(Alignment.Center)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {

            Column(Modifier.padding(defaultPadding)) {

                Text(
                    text = walletConfig.displayName ?: "",
                    color = uiErgoColor,
                    style = labelStyle(LabelStyle.HEADLINE2)
                )

                ChooseAddressButton(address, null, onChooseAddress)

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

                    IconButton(
                        onClick = {
                            address.publicAddress.copyToClipboard()
                            coroutineScope.launch {
                                scaffoldState?.snackbarHostState?.showSnackbar(
                                    Application.texts.getString(STRING_LABEL_COPIED),
                                )
                            }
                        },
                        Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                    }
                }
            }
        }
    }
}