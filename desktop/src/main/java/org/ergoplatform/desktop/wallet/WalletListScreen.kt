package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletConfig

@Composable
fun WalletListScreen(
    walletList: List<Wallet>,
    fiatValue: Float,
    isRefreshing: Boolean,
    onSendClicked: (String) -> Unit,
    onReceiveClicked: (WalletConfig) -> Unit,
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(walletList.size) { index ->

            WalletCard(walletList[index], fiatValue, onSendClicked, onReceiveClicked)

        }
    }

}