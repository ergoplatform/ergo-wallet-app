package org.ergoplatform.desktop.wallet

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import org.ergoplatform.appkit.Mnemonic
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.ui.showSensitiveDataCopyDialog

class CreateWalletComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = ""

    override val fullScreen: Boolean
        get() = true

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        CreateWalletScreen(
            onBack = {
                router.pop()
                mnemonic.erase()
            },
            onProceed = {
                router.push(ScreenConfig.ConfirmCreateWallet(mnemonic))
            },
            onCopy = {
                showSensitiveDataCopyDialog(navHost, mnemonic.toStringUnsecure())
            },
            remember { mnemonic.toStringUnsecure() }
        )
    }

    private val mnemonic = SecretString.create(Mnemonic.generateEnglishMnemonic())
}