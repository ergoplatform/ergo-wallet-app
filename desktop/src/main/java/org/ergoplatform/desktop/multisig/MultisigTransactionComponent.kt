package org.ergoplatform.desktop.multisig

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.compose.multisig.MultisigTxDetailsLayout
import org.ergoplatform.desktop.ui.AppScrollingLayout
import org.ergoplatform.desktop.ui.PasswordDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.proceedAuthFlowWithPassword
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_TITLE_MULTISIGTX_DETAILS
import org.ergoplatform.uilogic.multisig.MultisigTxDetailsUiLogic

class MultisigTransactionComponent(
    private val multisigId: Int,
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_MULTISIGTX_DETAILS)

    private val uiLogic = DesktopMultisigDetailsUiLogic().apply {
        initMultisigTx(multisigId, Application.database)
    }

    private var walletConfigForPassword: WalletConfig? by mutableStateOf(null)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        AppScrollingLayout {
            MultisigTxDetailsLayout(
                Modifier.align(Alignment.Center),
                uiLogic.multisigTx.collectAsState().value,
                uiLogic,
                onSignWith = { walletConfigForPassword = it },
                Application.texts,
                getDb = { Application.database }
            )
        }

        walletConfigForPassword?.let { walletConfig ->
            PasswordDialog(
                onDismissRequest = { walletConfigForPassword = null },
                onPasswordEntered = {
                    proceedAuthFlowWithPassword(it, walletConfig, ::proceedFromAuthFlow)
                }
            )
        }
    }

    private fun proceedFromAuthFlow(signingSecrets: SigningSecrets) {
        walletConfigForPassword?.let {
            uiLogic.signWith(it, signingSecrets)
        }
        walletConfigForPassword = null
    }

    private inner class DesktopMultisigDetailsUiLogic : MultisigTxDetailsUiLogic() {
        override val coroutineScope: CoroutineScope get() = componentScope()
    }
}