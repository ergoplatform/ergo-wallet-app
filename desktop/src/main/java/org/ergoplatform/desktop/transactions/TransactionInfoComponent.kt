package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.copyToClipboard
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.ui.openBrowser
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.transactions.ErgoPay
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.STRING_INFO_CANCEL_TX
import org.ergoplatform.uilogic.STRING_LABEL_COPIED
import org.ergoplatform.uilogic.STRING_TITLE_TRANSACTION
import org.ergoplatform.uilogic.STRING_ZXING_BUTTON_OK
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic
import org.ergoplatform.wallet.addresses.findWalletConfigAndAddressIdx

class TransactionInfoComponent(
    private val txId: String,
    private val address: String?,
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_TRANSACTION)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            if (canReloadButtonVisible.value && !loading.value) {
                IconButton(uiLogic::loadTx) {
                    Icon(
                        Icons.Default.Refresh,
                        null,
                    )
                }
            }
            IconButton({
                openBrowser(getExplorerTxUrl(txId))
            }) {
                Icon(
                    Icons.Default.OpenInBrowser,
                    null,
                )
            }
        }

    private val transactionInfoState = mutableStateOf<TransactionInfo?>(null)
    private val canReloadButtonVisible = mutableStateOf(false)
    private var canCancelTx by mutableStateOf(false)
    private val loading = mutableStateOf(true)

    private val uiLogic = DesktopTransactionInfoUiLogic().apply {
        loadTx()
    }

    private fun onCancelTx() {
        navHost.dialogHandler.showDialog(
            MosaikDialog(
                Application.texts.getString(STRING_INFO_CANCEL_TX),
                Application.texts.getString(STRING_ZXING_BUTTON_OK),
                positiveButtonClicked = ::doCancelTx
            )
        )
    }

    private fun doCancelTx() {
        componentScope().launch {
            val prompt = uiLogic.buildCancelTransaction(Application.prefs, Application.texts)

            if (!prompt.success)
                navHost.showErrorDialog(prompt.errorMsg!!)
            else {
                // find wallet to use
                val walletAndIdx = findWalletConfigAndAddressIdx(
                    address!!, Application.database.walletDbProvider
                )

                navHost.router.push(
                    ScreenConfig.ErgoPay(
                        ErgoPay.buildStaticSigningRequest(prompt.serializedTx!!),
                        walletAndIdx?.first, walletAndIdx?.second,
                    )
                )
            }
        }
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        TransactionInfoScreen(
            transactionInfoState.value,
            loading.value,
            uiLogic,
            onTxIdClicked = {
                txId.copyToClipboard()
                navHost.showSnackbar(Application.texts.getString(STRING_LABEL_COPIED))
            },
            onTokenClick = { tokenId -> router.push(ScreenConfig.TokenInformation(tokenId)) },
            onCancelClicked = if (canCancelTx) ::onCancelTx else null,
        )
    }

    private inner class DesktopTransactionInfoUiLogic : TransactionInfoUiLogic() {
        override val coroutineScope: CoroutineScope get() = componentScope()

        override fun onTransactionInformationFetched(ti: TransactionInfo?) {
            loading.value = isLoading
            transactionInfoState.value = ti
            canReloadButtonVisible.value = shouldOfferReloadButton()
            canCancelTx = shouldOfferCancelButton()
        }

        fun loadTx() {
            init(
                this@TransactionInfoComponent.txId,
                address,
                ApiServiceManager.getOrInit(Application.prefs),
                Application.database,
                forceReload = true,
            )
            loading.value = isLoading
        }
    }
}