package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.desktop.ui.PasswordDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.proceedAuthFlowWithPassword
import org.ergoplatform.desktop.wallet.WalletConfigComponent
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_ADDRESSES
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressesUiLogic

class WalletAddressesComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val walletConfig: WalletConfig,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {
    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_WALLET_ADDRESSES) + " " +
                walletConfig.displayName

    private val addressesList = mutableStateOf(emptyList<WalletAddress>())
    private val passwordAddAddresses = mutableStateOf(0)

    private val uiLogic = object : WalletAddressesUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyNewAddresses() {
            addressesList.value = addresses
        }

        override fun notifyUiLocked(locked: Boolean) {
            navHost.lockScreen.value = locked
        }
    }.apply {
        init(Application.database.walletDbProvider, walletConfig.id)
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        WalletAddressesScreen(
            addressesList.value,
            uiLogic.wallet,
            if (uiLogic.canDeriveAddresses()) { num ->
                addAddresses(num)
            } else null,
            onOpenDetails = {
                // TODO addresses
            }
        )


        if (passwordAddAddresses.value > 0) {
            PasswordDialog(
                onDismissRequest = { passwordAddAddresses.value = 0 },
                onPasswordEntered = {
                    proceedAuthFlowWithPassword(it, walletConfig, ::proceedFromAuthFlow)
                }
            )
        }
    }

    private fun addAddresses(num: Int) {
        if (walletConfig.extendedPublicKey != null)
            uiLogic.addNextAddresses(
                Application.database.walletDbProvider,
                Application.prefs,
                num,
                null
            )
        else
            passwordAddAddresses.value = num
    }

    private fun proceedFromAuthFlow(signingSecrets: SigningSecrets) {
        uiLogic.addNextAddresses(
            Application.database.walletDbProvider,
            Application.prefs,
            passwordAddAddresses.value,
            signingSecrets
        )
    }
}