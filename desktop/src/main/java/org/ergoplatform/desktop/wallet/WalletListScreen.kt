package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.LinearProgressIndicator
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

    if (isRefreshing) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    val state = rememberLazyListState()
    Box {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            items(walletList.size) { index ->

                WalletCard(walletList[index], fiatValue, onSendClicked, onReceiveClicked)

            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}