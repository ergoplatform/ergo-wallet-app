package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.ergoplatform.desktop.ui.scrollbar
import org.ergoplatform.mosaik.MosaikStyleConfig
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

    if (isRefreshing) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().scrollbar(
            state,
            false,
            hiddenAlpha = 1f,
            knobColor = MosaikStyleConfig.secondaryLabelColor,
            trackColor = Color.Transparent
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        items(walletList.size) { index ->

            WalletCard(walletList[index], fiatValue, onSendClicked, onReceiveClicked)

        }
    }

}