package org.ergoplatform.desktop.wallet

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.Application
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.desktop.ui.AppLockScreen
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.uilogic.wallet.SaveWalletUiLogic

class SaveWalletComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val mnemonic: SecretString,
    private val fromRestore: Boolean,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = ""

    override val fullScreen: Boolean
        get() = true

    private var publicAddress = mutableStateOf<String?>(null)
    private var derivedAddressNum = mutableStateOf(0)
    private var walletNameTextValue = mutableStateOf<TextFieldValue?>(null)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val address = publicAddress.value
        if (address != null)
            SaveWalletScreen(
                address,
                uiLogic!!.hasAlternativeAddress,
                derivedAddressNum.value,
                walletNameTextValue,
                onBack = router::pop,
                onProceed = {
                    // TODO
                },
                onUseAltAddress = { switchAddress() }
            )
        AppLockScreen(address == null)

    }

    private var uiLogic: SaveWalletUiLogic? = null

    init {
        componentScope().launch(Dispatchers.Default) {
            uiLogic = SaveWalletUiLogic(mnemonic, fromRestore)
            newAddress()

            startDerivedAddressesSearch()
        }
        lifecycle.doOnDestroy {
            mnemonic.erase()
        }
    }

    private fun switchAddress() {
        uiLogic?.let { uiLogic ->
            uiLogic.switchAddress()
            componentScope().launch(Dispatchers.Default) {
                newAddress()
                startDerivedAddressesSearch()
            }
        }
    }

    private suspend fun newAddress() {
        val db = Application.database.walletDbProvider
        val walletDisplayName = uiLogic!!.getSuggestedDisplayName(
            db,
            Application.texts
        )
        val showDisplayName = uiLogic!!.showSuggestedDisplayName(db)

        publicAddress.value = uiLogic!!.publicAddress
        walletNameTextValue.value = if (showDisplayName) TextFieldValue(walletDisplayName) else null

    }

    private suspend fun startDerivedAddressesSearch() {
        uiLogic!!.startDerivedAddressesSearch(
            ApiServiceManager.getOrInit(Application.prefs),
            Application.database.walletDbProvider
        ) { derivedAddressNum.value = it }
    }
}