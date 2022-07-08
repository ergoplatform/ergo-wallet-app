package org.ergoplatform.desktop.wallet

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.uilogic.wallet.RestoreWalletUiLogic

class RestoreWalletComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = ""

    override val fullScreen: Boolean
        get() = true

    private val mnemonicTextState = mutableStateOf(TextFieldValue())
    private val hintTextState = mutableStateOf("")

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {

        RestoreWalletScreen(
            onBack = router::pop,
            onProceed = {
                uiLogic.doRestore()
            },
            onTextChange = uiLogic::userChangedMnemonic,
            mnemonicTextState,
            hintTextState.value,
        )
    }

    val uiLogic = object : RestoreWalletUiLogic(Application.texts) {

        override fun getEnteredMnemonic(): CharSequence = mnemonicTextState.value.text
        override fun setErrorLabel(error: String?) {
            hintTextState.value = error ?: ""
        }

        override fun navigateToSaveWalletDialog(mnemonic: String) {
            // TODO
        }

        override fun hideForcedSoftKeyboard() {
        }
    }
}