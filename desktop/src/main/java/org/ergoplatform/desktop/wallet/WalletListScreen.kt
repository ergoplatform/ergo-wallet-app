package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.ergoplatform.persistance.Wallet

@Composable
fun WalletListScreen(
    walletList: List<Wallet>,
    onGoClicked: (String) -> Unit,
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(walletList.size) { index ->

            WalletCard(walletList[index], onGoClicked)

        }
    }

}