package org.ergoplatform.desktop.wallet

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.push
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.uilogic.wallet.ConfirmCreateWalletUiLogic

class ConfirmCreateWalletComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent,
    private val mnemonic: SecretString,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = ""

    override val fullScreen: Boolean
        get() = true

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val errorWords = remember { mutableStateOf(listOf(false, false)) }

        ConfirmCreateWalletScreen(
            onBack = router::pop,
            onProceed = { word1, word2, check ->
                val hadErrors = uiLogic.checkUserEnteredCorrectWordsAndConfirmedObligations(
                    word1,
                    word2,
                    check
                )

                errorWords.value = listOf(
                    !uiLogic.firstWordCorrect,
                    !uiLogic.secondWordCorrect
                )

                if (!hadErrors) {
                    router.push(ScreenConfig.SaveWallet(mnemonic, false))
                }
            },
            word1Num = uiLogic.firstWord,
            word2Num = uiLogic.secondWord,
            errorWords = errorWords.value,
        )
    }

    private val uiLogic = ConfirmCreateWalletUiLogic().apply {
        mnemonic = this@ConfirmCreateWalletComponent.mnemonic
    }
}