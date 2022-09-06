package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.desktop.ui.defaultMaxWidth
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.wallet.AddressTransactionInfo
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressButton
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.transactions.TransactionListManager
import org.ergoplatform.uilogic.STRING_TRANSACTIONS_LOAD_ALL_BUTTON
import org.ergoplatform.uilogic.STRING_TRANSACTIONS_LOAD_ALL_DESC
import org.ergoplatform.uilogic.STRING_TRANSACTIONS_NONE_YET
import org.ergoplatform.uilogic.STRING_TX_DOWNLOAD_PROGRESS
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens

@Composable
fun AddressTransactionsScreen(
    wallet: Wallet,
    walletAddress: WalletAddress,
    onChooseAddressClicked: () -> Unit
) {
    val progressState = TransactionListManager.downloadProgress.collectAsState()

    Column(
        Modifier.padding(top = defaultPadding).fillMaxWidth()
    ) {
        ChooseAddressButton(
            walletAddress,
            wallet,
            onClick = onChooseAddressClicked,
            style = LabelStyle.HEADLINE2,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        val address = TransactionListManager.downloadAddress.value
        if (progressState.value > 0 && address == walletAddress.publicAddress) {

            Text(
                Application.texts.getString(STRING_TX_DOWNLOAD_PROGRESS, progressState.value),
                Modifier.fillMaxWidth().padding(top = defaultPadding),
                textAlign = TextAlign.Center,

                )

        }

        val isDownloading = TransactionListManager.isDownloading.collectAsState()
        if (isDownloading.value) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = defaultPadding))
        }

        val listState = rememberLazyListState()
        val shownListState =
            remember(walletAddress) { mutableStateOf(emptyList<AddressTransactionWithTokens>()) }

        if (shownListState.value.isNotEmpty())
            Box {
                LazyColumn(
                    Modifier.padding(top = defaultPadding / 2),
                    listState
                ) {
                    items(shownListState.value.size + 1) { index ->

                        if (index < shownListState.value.size) {
                            val transactionWithTokens = shownListState.value[index]

                            key(transactionWithTokens) {
                                Box(Modifier.fillMaxWidth()) {
                                    AppCard(
                                        Modifier.padding(defaultPadding / 2)
                                            .widthIn(min = 400.dp, max = defaultMaxWidth)
                                            .align(Alignment.Center)
                                    ) {
                                        AddressTransactionInfo(transactionWithTokens)
                                    }
                                }
                            }
                        } else
                            LoadAllLayout(walletAddress)

                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = listState
                    )
                )
            }
        else {
            Text(
                remember { Application.texts.getString(STRING_TRANSACTIONS_NONE_YET) },
                Modifier.widthIn(max = defaultMaxWidth).align(Alignment.CenterHorizontally)
                    .padding(vertical = defaultPadding * 4),
                textAlign = TextAlign.Center,
            )
        }

        val pageLimit = 50
        val itemsToLoad = remember(walletAddress) { mutableStateOf(pageLimit) }

        LaunchedEffect(
            itemsToLoad.value,
            walletAddress,
            TransactionListManager.isDownloading.value,
            TransactionListManager.downloadProgress.value,
        ) {
            shownListState.value =
                Application.database.transactionDbProvider.loadAddressTransactionsWithTokens(
                    walletAddress.publicAddress,
                    itemsToLoad.value, 0
                )

        }

        listState.OnBottomReached {
            if (shownListState.value.size >= itemsToLoad.value)
                itemsToLoad.value = itemsToLoad.value + pageLimit
        }

    }

}

@Composable
private fun LoadAllLayout(walletAddress: WalletAddress) {
    Column(Modifier.fillMaxWidth().padding(vertical = defaultPadding)) {

        Text(
            remember { Application.texts.getString(STRING_TRANSACTIONS_LOAD_ALL_DESC) },
            Modifier.padding(defaultPadding * 2)
                .align(Alignment.CenterHorizontally).widthIn(max = defaultMaxWidth),
            textAlign = TextAlign.Center
        )

        AppButton(
            onClick = {
                TransactionListManager.startDownloadAllAddressTransactions(
                    walletAddress.publicAddress,
                    ApiServiceManager.getOrInit(Application.prefs),
                    Application.database,
                )
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(remember { Application.texts.getString(STRING_TRANSACTIONS_LOAD_ALL_BUTTON) })
        }
    }
}

@Composable
fun LazyListState.OnBottomReached(
    loadMore: () -> Unit
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf true

            lastVisibleItem.index == layoutInfo.totalItemsCount - 1
        }
    }

    // Convert the state into a cold flow and collect
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .collect {
                // if should load more, then invoke loadMore
                if (it) loadMore()
            }
    }
}