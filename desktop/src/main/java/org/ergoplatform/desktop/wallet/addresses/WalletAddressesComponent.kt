package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.SigningSecrets
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.desktop.ui.PasswordDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.proceedAuthFlowWithPassword
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_ADDRESSES
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressDialogUiLogic
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
    private val walletState = mutableStateOf<Wallet?>(null)
    private val passwordAddAddresses = mutableStateOf(0)
    private val detailsScreenState = mutableStateOf<WalletAddress?>(null)

    private val uiLogic = object : WalletAddressesUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyNewAddresses() {
            addressesList.value = addresses
            walletState.value = wallet
        }

        override fun notifyUiLocked(locked: Boolean) {
            navHost.lockScreen.value = locked
        }
    }

    private val detailsUiLogic = WalletAddressDialogUiLogic()

    init {
        val nodeConnector = WalletStateSyncManager.getInstance()
        componentScope().launch {
            // balance refresh for newly added addresses needs to be triggered by
            // observing NodeConnector refresh. Since the singleAddressRefresh will
            // always return its last saved value, initializing can be done here, too
            nodeConnector.singleAddressRefresh.collect {
                uiLogic.init(Application.database.walletDbProvider, walletConfig.id)
            }
        }
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        WalletAddressesScreen(
            addressesList.value,
            walletState.value,
            if (uiLogic.canDeriveAddresses()) { num ->
                addAddresses(num)
            } else null,
            onOpenDetails = { detailsScreenState.value = it }
        )

        val detailsAddress = detailsScreenState.value
        if (detailsAddress != null) {
            WalletAddressDetailsDialog(detailsAddress, scaffoldState,
                onChangeName = { address, newName ->
                    componentScope().launch {
                        detailsUiLogic.saveWalletAddressLabel(
                            Application.database.walletDbProvider,
                            address.id,
                            newName.ifEmpty { null }
                        )
                    }
                    detailsScreenState.value = null
                },
                onDeleteAddress = {
                    componentScope().launch {
                        detailsUiLogic.deleteWalletAddress(Application.database, it.id)
                    }
                    detailsScreenState.value = null
                },
                onDismiss = { detailsScreenState.value = null }
            )
        }

        if (passwordAddAddresses.value > 0) {
            PasswordDialog(
                onDismissRequest = { passwordAddAddresses.value = 0 },
                onPasswordEntered = { password ->
                    // password is erased by [PasswordDialog] when this method completed
                    proceedAuthFlowWithPassword(password, walletConfig, ::proceedFromAuthFlow)
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