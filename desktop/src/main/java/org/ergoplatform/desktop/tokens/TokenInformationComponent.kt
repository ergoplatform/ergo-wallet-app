package org.ergoplatform.desktop.tokens

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.persistance.GENUINE_UNKNOWN
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NONE
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.uilogic.STRING_LABEL_ADD_TOKEN
import org.ergoplatform.uilogic.tokens.TokenInformationLayoutLogic
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic

class TokenInformationComponent(
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
    private val tokenId: String,
    private val balance: Long?
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = Application.texts.getString(STRING_LABEL_ADD_TOKEN) + " " + tokenInfoState.value?.displayName

    private val uiLogic = DesktopTokenInformationLogic().apply {
        init(tokenId, Application.database, Application.prefs)
    }

    private val tokenInfoState = mutableStateOf<DesktopTokenInformationLayoutLogic?>(null)
    private val tokenInfoLoading = mutableStateOf(true)
    private val downloadState = mutableStateOf(uiLogic.downloadState)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        TokenInformationScreen(
            tokenInfoState.value,
            tokenInfoLoading.value,
            downloadState.value,
            startDownload = { uiLogic.downloadContent(Application.prefs) }
        )
    }

    inner class DesktopTokenInformationLogic : TokenInformationModelLogic() {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun onTokenInfoUpdated(tokenInformation: TokenInformation?) {
            val infoLayoutLogic = DesktopTokenInformationLayoutLogic().apply {
                updateLayout(uiLogic, Application.texts, balance ?: 0)
                this.tokenInformation = tokenInformation
            }
            tokenInfoState.value = infoLayoutLogic
            tokenInfoLoading.value = false
            this@TokenInformationComponent.downloadState.value = downloadState
        }

        override fun onDownloadStateUpdated() {
            tokenInfoState.value?.updateNftPreview(uiLogic)
            this@TokenInformationComponent.downloadState.value = downloadState
        }
    }

    class DesktopTokenInformationLayoutLogic : TokenInformationLayoutLogic() {
        var displayName: String? = null
        var tokenId: String? = null
        var description: String? = null

        var balanceAmount: String? = null
        var balanceValue: String? = null

        var tokenInformation: TokenInformation? = null

        var supplyAmount: String? = null

        var tokenGenuineFlag: Int = GENUINE_UNKNOWN

        var nftLayoutVisible = false
        var nftContentLink: String? = null
        var nftContentHash: String? = null
        var nftThumnailType: Int = THUMBNAIL_TYPE_NONE

        var nftHashValidated: Boolean? = null
        var nftDownloadPercent: Float = 0f
        var nftDownloadContent: ByteArray? = null

        override fun setTokenTextFields(displayName: String, tokenId: String, description: String) {
            this.displayName = displayName
            this.tokenId = tokenId
            this.description = description
        }

        override fun setLabelSupplyAmountText(supplyAmount: String?) {
            this.supplyAmount = supplyAmount
        }

        override fun setTokenGenuine(genuineFlag: Int) {
            this.tokenGenuineFlag = genuineFlag
        }

        override fun setBalanceAmountAndValue(amount: String?, balanceValue: String?) {
            this.balanceValue = balanceValue
            balanceAmount = amount
        }

        override fun setContentLinkText(linkText: String) {
            nftContentLink = linkText
        }

        override fun setNftLayoutVisibility(visible: Boolean) {
            nftLayoutVisible = visible
        }

        override fun setContentHashText(hashText: String) {
            nftContentHash = hashText
        }

        override fun setThumbnail(thumbnailType: Int) {
            nftThumnailType = thumbnailType
        }

        override fun showNftPreview(
            downloadState: TokenInformationModelLogic.StateDownload,
            downloadPercent: Float,
            content: ByteArray?
        ) {
            nftDownloadContent = content
            nftDownloadPercent = downloadPercent
        }

        override fun showNftHashValidation(hashValid: Boolean?) {
            nftHashValidated = hashValid
        }

    }
}