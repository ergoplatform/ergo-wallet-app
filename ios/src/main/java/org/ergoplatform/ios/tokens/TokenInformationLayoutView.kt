package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.tokens.TokenInformationLayoutLogic
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSTextAlignment
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UIView

class TokenInformationLayoutView : UIView(CGRect.Zero()) {
    private val layoutLogic = IosTokenInformationLayoutLogic()
    private val nameLabel = Headline2Label().apply {
        numberOfLines = 1
        textAlignment = NSTextAlignment.Center
    }
    private val genuineImageContainer = GenuineImageContainer()

    init {
        val topStack = UIStackView(NSArray(nameLabel, genuineImageContainer)).apply {
            axis = UILayoutConstraintAxis.Horizontal
        }

        addSubview(topStack)
        topStack.topToSuperview(topInset = DEFAULT_MARGIN).centerHorizontal(true)
    }

    fun updateTokenInformation(modelLogic: TokenInformationModelLogic, balanceAmount: Long?) {
        layoutLogic.updateLayout(modelLogic, IosStringProvider(getAppDelegate().texts), balanceAmount ?: 0)
    }

    inner class IosTokenInformationLayoutLogic : TokenInformationLayoutLogic() {
        override fun setTokenTextFields(displayName: String, tokenId: String, description: String) {
            nameLabel.text = displayName
        }

        override fun setLabelSupplyAmountText(supplyAmount: String?) {
            // TODO token
        }

        override fun setTokenGenuine(genuineFlag: Int) {
            genuineImageContainer.setGenuineFlag(genuineFlag)
        }

        override fun setBalanceAmountAndValue(amount: String?, balanceValue: String?) {
            // TODO token
        }

        override fun setContentLinkText(linkText: String) {
            // todo token
        }

        override fun setNftLayoutVisibility(visible: Boolean) {
            // TODO("Not yet implemented")
        }

        override fun setContentHashText(hashText: String) {
            // TODO("Not yet implemented")
        }

        override fun setThumbnail(thumbnailType: Int) {
            // TODO("Not yet implemented")
        }

        override fun showNftPreview(
            downloadState: TokenInformationModelLogic.StateDownload,
            downloadPercent: Float,
            content: ByteArray?
        ) {
            // TODO("Not yet implemented")
        }

        override fun showNftHashValidation(hashValid: Boolean?) {
            // TODO("Not yet implemented")
        }

    }
}