package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressesListDialog
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.TransactionListManager
import org.ergoplatform.transactions.TransactionsExport
import org.ergoplatform.uilogic.STRING_LABEL_ERROR_OCCURED
import org.ergoplatform.uilogic.STRING_TITLE_TRANSACTIONS
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.wallet.getDerivedAddressEntity
import java.io.File

class AddressTransactionsComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val walletConfig: WalletConfig,
    private val derivationIdx: Int,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_TRANSACTIONS) + " " + walletConfig.displayName

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton(::refreshAddress) {
                Icon(Icons.Default.Refresh, null)
            }

            IconButton(::exportList) {
                Icon(Icons.Default.SaveAlt, null)
            }
        }

    private val chooseAddressDialog = mutableStateOf(false)
    private var wallet: Wallet? = null
    private var shownAddress: WalletAddress? = null
    private val shownListState = mutableStateOf(emptyList<AddressTransactionWithTokens>())


    init {
        lifecycle.doOnResume {
            if (wallet == null) {
                runBlocking {
                    Application.database.walletDbProvider.loadWalletWithStateById(walletConfig.id)
                        ?.let { wallet ->
                            this@AddressTransactionsComponent.wallet = wallet
                            shownAddress = wallet.getDerivedAddressEntity(derivationIdx)
                            refreshAddress()
                        }
                }
            } else {
                refreshAddress()
            }
        }
    }

    private fun exportList() {
        componentScope().launch {
            navHost.lockScreen.value = true
            try {
                val transactions = shownListState.value
                val filename = Application.dataDir.trimEnd('/', '\\') +
                        "/transactions_${System.currentTimeMillis()}.csv"
                TransactionsExport.exportTransactions(transactions) { File(filename).bufferedWriter() }
                navHost.showErrorDialog("Content of ${transactions.size} transactions exported to ${filename}. " +
                        "If you want to export more into the past, scroll the list to the point in time.")
            } catch (t: Throwable) {
                navHost.showErrorDialog(
                    Application.texts.getString(STRING_LABEL_ERROR_OCCURED, t.message ?: "Unknown")
                )
            }
            navHost.lockScreen.value = false
        }
    }

    private fun refreshAddress() {
        TransactionListManager.downloadTransactionListForAddress(
            shownAddress!!.publicAddress,
            ApiServiceManager.getOrInit(Application.prefs),
            Application.database
        )
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        AddressTransactionsScreen(
            wallet!!,
            shownAddress!!,
            shownListState,
            onChooseAddressClicked = { chooseAddressDialog.value = true },
            onTransactionClicked = { addressTx ->
                router.push(ScreenConfig.TransactionInfo(addressTx.txId, addressTx.address))
            },
            onTokenClicked = { router.push(ScreenConfig.TokenInformation(it)) }
        )

        if (chooseAddressDialog.value) {
            ChooseAddressesListDialog(
                wallet!!,
                false,
                onAddressChosen = { walletAddress ->
                    shownAddress = walletAddress
                    chooseAddressDialog.value = false
                },
                onDismiss = { chooseAddressDialog.value = false },
            )
        }

    }
}