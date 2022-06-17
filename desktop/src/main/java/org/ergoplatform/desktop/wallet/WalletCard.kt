package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.tsBody1
import org.ergoplatform.desktop.ui.tsHeadline1
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.wallet.getBalanceForAllAddresses

@Composable
fun WalletCard(wallet: Wallet, onSendClicked: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(16.dp).defaultMinSize(400.dp, 200.dp).widthIn(max = 600.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {

                Icon(
                    Icons.Default.AccountBalanceWallet,
                    null,
                    Modifier.align(Alignment.Top).size(58.dp)
                )

                Column(
                    modifier = Modifier.align(Alignment.Top).padding(start = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    Text(
                        text = wallet.walletConfig.displayName!!,
                        color = uiErgoColor,
                        style = tsBody1.copy(fontWeight = FontWeight.Bold)
                    )

                    val balanceErgoAmount = ErgoAmount(wallet.getBalanceForAllAddresses())

                    Text(
                        text = balanceErgoAmount.toStringRoundToDecimals(),
                        style = tsHeadline1
                    )

                    val walletSyncManager = WalletStateSyncManager.getInstance()
                    if (walletSyncManager.hasFiatValue) {

                        Text(
                            text = formatFiatToString(
                                balanceErgoAmount.toDouble() * walletSyncManager.fiatValue.value,
                                walletSyncManager.fiatCurrency, Application.texts
                            ),
                            style = tsHeadline1
                        )


                    }

                }

            }

            Row(Modifier.padding(top = 16.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        MosaikStyleConfig.secondaryButtonColor,
                        MosaikStyleConfig.secondaryButtonTextColor
                    )
                ) {
                    Text(Application.texts.getString(STRING_BUTTON_RECEIVE))
                }
                Box(Modifier.size(16.dp))
                Button(
                    onClick = { onSendClicked(wallet.walletConfig.displayName!!) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        MosaikStyleConfig.primaryLabelColor,
                        MosaikStyleConfig.primaryButtonTextColor
                    )
                ) {
                    Text(Application.texts.getString(STRING_BUTTON_SEND))
                }
            }
        }
    }

}