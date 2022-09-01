package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.mediumIconSize
import org.ergoplatform.compose.settings.smallIconSize
import org.ergoplatform.desktop.ui.AppScrollingLayout
import org.ergoplatform.desktop.ui.defaultMaxWidth
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressButton
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_ADDRESS

@Composable
fun WalletDetailsScreen(
    wallet: Wallet,
    walletAddress: WalletAddress?,
    onScanClicked: () -> Unit,
    onChooseAddressClicked: () -> Unit,
    onReceiveClicked: () -> Unit,
    onSendClicked: () -> Unit,
    onAddressesClicked: () -> Unit,
) {
    AppScrollingLayout {
        Column(
            Modifier.align(Alignment.TopCenter)
                .widthIn(min = 400.dp, max = defaultMaxWidth)
        ) {

            AppCard(Modifier.padding(vertical = defaultPadding)) {
                Column(Modifier.padding(defaultPadding)) {

                    Row {
                        Icon(
                            Icons.Default.AltRoute,
                            null,
                            Modifier.requiredSize(mediumIconSize).align(Alignment.CenterVertically)
                        )

                        Column(Modifier.weight(1f).padding(start = defaultPadding)) {

                            Text(remember { Application.texts.getString(STRING_TITLE_WALLET_ADDRESS) })

                            ChooseAddressButton(
                                walletAddress,
                                wallet,
                                onClick = onChooseAddressClicked
                            )

                        }

                        IconButton(onScanClicked, Modifier.align(Alignment.Top)) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                null,
                                Modifier.requiredSize(smallIconSize)
                            )
                        }
                    }

                    Row(Modifier.padding(top = defaultPadding / 2)) {
                        Box(Modifier.requiredWidth(mediumIconSize))

                        IconButton(onReceiveClicked, Modifier.weight(1f)) {
                            Icon(Icons.Default.CallReceived, null)
                        }

                        IconButton(onSendClicked, Modifier.weight(1f)) {
                            Icon(Icons.Default.CallMade, null)
                        }

                        IconButton(onAddressesClicked, Modifier.weight(1f)) {
                            Icon(Icons.Default.FormatListNumbered, null)
                        }

                    }
                }
            }

        }
    }
}