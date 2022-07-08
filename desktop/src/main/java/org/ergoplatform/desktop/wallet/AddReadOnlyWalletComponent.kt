package org.ergoplatform.desktop.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.popWhile
import com.arkivanov.decompose.router.push
import kotlinx.coroutines.launch
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.uilogic.STRING_LABEL_READONLY_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_READONLY_WALLET_DEFAULT
import org.ergoplatform.uilogic.wallet.AddReadOnlyWalletUiLogic

class AddReadOnlyWalletComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = "" // no app bar shown

    override val fullScreen: Boolean
        get() = true

    private val walletAddress = mutableStateOf(TextFieldValue())
    private val walletName = mutableStateOf(
        TextFieldValue(
            Application.texts.getString(STRING_LABEL_READONLY_WALLET_DEFAULT)
        )
    )
    private val errorMessage = mutableStateOf<String?>(null)

    private val uiLogic = object : AddReadOnlyWalletUiLogic() {
        override fun setErrorMessage(message: String) {
            errorMessage.value = message
        }
    }

    @Composable
    override fun renderScreenContents() {
        AddReadOnlyWalletScreen(
            walletAddress, walletName, errorMessage,
            onScanAddress = {
                router.push(ScreenConfig.QrCodeScanner { qrCode ->
                    walletAddress.value = TextFieldValue(uiLogic.getInputFromQrCode(qrCode))
                })
            },
            onAddClicked = {
                componentScope().launch {
                    val success = uiLogic.addWalletToDb(
                        walletAddress.value.text,
                        Application.database.walletDbProvider,
                        Application.texts,
                        walletName.value.text
                    )

                    if (success)
                        router.popWhile { !(it is ScreenConfig.WalletList) }
                }
            },
            onBack = router::pop
        )
    }
}