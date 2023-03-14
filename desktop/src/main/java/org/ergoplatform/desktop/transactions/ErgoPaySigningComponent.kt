package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.wallet.ChooseWalletListDialog
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_TITLE_ERGO_PAY_REQUEST
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic
import org.ergoplatform.uilogic.transactions.SubmitTransactionUiLogic
import org.ergoplatform.wallet.isMultisig

class ErgoPaySigningComponent(
    private val request: String,
    private val walletId: Int?,
    private val derivationIndex: Int?,
    private val doOnComplete: (() -> Unit),
    componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : SubmitTransactionComponent(componentContext, navHost) {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_TITLE_ERGO_PAY_REQUEST)

    private val canReloadState = mutableStateOf(false)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton(
                {
                    ergoPayUiLogic.reloadFromDapp(
                        Application.prefs,
                        Application.texts,
                        Application.database.walletDbProvider
                    )
                },
                enabled = canReloadState.value,
            ) {
                Icon(Icons.Default.Refresh, null)
            }
        }

    private val ergoPayUiLogic = DesktopErgoPayUiLogic()

    override val uiLogic: SubmitTransactionUiLogic = ergoPayUiLogic

    private val ergoPayState = mutableStateOf(ErgoPaySigningUiLogic.State.FETCH_DATA)
    private val chooseWalletDialog = mutableStateOf<List<Wallet>?>(null)

    init {
        ergoPayUiLogic.init(
            request,
            walletId ?: -1,
            derivationIndex ?: -1,
            Application.database.walletDbProvider,
            Application.prefs,
            Application.texts,
        )
    }

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        ErgoPaySigningScreen(
            ergoPayState.value,
            ergoPayUiLogic,
            onReload = {
                ergoPayUiLogic.reloadFromDapp(
                    Application.prefs,
                    Application.texts,
                    Application.database.walletDbProvider
                )
            },
            onChooseAddress = {
                if (uiLogic.wallet != null) {
                    startChooseAddress(withAllAddresses = ergoPayUiLogic.addressRequestCanHandleMultiple)
                } else {
                    startChooseWallet()
                }
            },
            onConfirm = ::startPayment,
            onTokenClick = { tokenId -> router.push(ScreenConfig.TokenInformation(tokenId)) },
            onDismiss = router::pop,
        )
        if (chooseWalletDialog.value != null)
            ChooseWalletListDialog(
                chooseWalletDialog.value!!, true,
                onWalletChosen = { walletConfig ->
                    chooseWalletDialog.value = null
                    ergoPayUiLogic.setWalletId(
                        walletConfig.id,
                        Application.prefs,
                        Application.texts,
                        Application.database.walletDbProvider
                    )
                },
                onDismiss = { chooseWalletDialog.value = null },
            )

        SubmitTransactionOverlays()
    }

    private fun startChooseWallet() {
        componentScope().launch {
            chooseWalletDialog.value = Application.database.walletDbProvider.getWalletsWithStates()
        }
    }

    override fun onAddressChosen(derivationIndex: Int?) {
        super.onAddressChosen(derivationIndex)
        // redo the request - can't be done within uilogic because context is needed on Android
        ergoPayUiLogic.derivedAddressIdChanged(
            Application.prefs,
            Application.texts,
            Application.database.walletDbProvider
        )
    }

    private inner class DesktopErgoPayUiLogic : ErgoPaySigningUiLogic() {
        override fun notifyStateChanged(newState: State) {
            ergoPayState.value = newState
            canReloadState.value = ergoPayUiLogic.canReloadFromDapp()

            if (newState == State.DONE && ergoPayUiLogic.txId != null) {
                doOnComplete()
            }
        }

        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun notifyWalletStateLoaded() {
            // not needed, notifyDerivedAddressChanged is always called right after this one
        }

        override fun notifyDerivedAddressChanged() {
            // TODO show selected address at top of screen
        }

        override fun notifyUiLocked(locked: Boolean) {
            navHost.lockScreen.value = locked
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            if (!txResult.success) {
                navHost.showErrorDialog(getTransactionResultErrorMessage(txResult))
            } else if (wallet!!.isMultisig()) {
                // TODO 167 switch to multisig details page
                println("New tx id: " + multisigTransactionId!!)
            }
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            showSigningPrompt(signingPrompt)
        }

    }
}