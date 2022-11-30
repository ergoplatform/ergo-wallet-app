package org.ergoplatform.desktop.transactions

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.wallet.addresses.ChooseAddressesListDialog
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic

class ReceiveToWalletComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
    private val walletConfig: WalletConfig,
    addressIdx: Int,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_BUTTON_RECEIVE)

    private val uiLogic = ReceiveToWalletUiLogic()

    init {
        componentScope().launch {
            uiLogic.loadWallet(walletConfig.id, Application.database.walletDbProvider)
            if (addressIdx != uiLogic.derivationIdx)
                uiLogic.derivationIdx = addressIdx
            addressState.value = uiLogic.address
        }
    }

    private val chooseAddressDialogState = mutableStateOf(false)
    private val addressState = mutableStateOf<WalletAddress?>(null)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        addressState.value?.let { walletAddress ->
            ReceiveToWalletScreen(
                walletConfig, walletAddress,
                onChooseAddress = {
                    chooseAddressDialogState.value = true
                },
                scaffoldState
            )
        }

        if (chooseAddressDialogState.value) {
            ChooseAddressesListDialog(uiLogic.wallet!!, false, onAddressChosen = {
                uiLogic.derivationIdx = it?.derivationIndex ?: 0
                chooseAddressDialogState.value = false
                addressState.value = uiLogic.address
            },
                onDismiss = {
                    chooseAddressDialogState.value = false
                })
        }
    }
}