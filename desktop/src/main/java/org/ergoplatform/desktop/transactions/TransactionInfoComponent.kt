package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.copyToClipboard
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.ui.openBrowser
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.STRING_LABEL_COPIED
import org.ergoplatform.uilogic.STRING_TITLE_TRANSACTION
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic

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
    private val loading = mutableStateOf(true)

    private val uiLogic = object : TransactionInfoUiLogic() {
        override val coroutineScope: CoroutineScope get() = componentScope()

        override fun onTransactionInformationFetched(ti: TransactionInfo?) {
            loading.value = false
            transactionInfoState.value = ti
        }
    }.apply {
        init(
            this@TransactionInfoComponent.txId,
            address,
            ApiServiceManager.getOrInit(Application.prefs),
            Application.database
        )
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
            onTokenClick = { tokenId -> router.push(ScreenConfig.TokenInformation(tokenId)) }
        )
    }
}