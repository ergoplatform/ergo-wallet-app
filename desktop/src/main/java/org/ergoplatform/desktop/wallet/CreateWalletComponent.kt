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

    /**
     * this mnemonic object is shared passed through and shared between the onboarding screens
     * CreateWallet -> ConfirmCreateWallet -> SaveWallet. It is stored so that back/forth
     * navigating of the user won't change it. It is ultimately erased in
     * [SaveWalletComponent.saveToDbAndNavigateToWallet] after encryption.
     */
    private val mnemonic = SecretString.create(Mnemonic.generateEnglishMnemonic())
}