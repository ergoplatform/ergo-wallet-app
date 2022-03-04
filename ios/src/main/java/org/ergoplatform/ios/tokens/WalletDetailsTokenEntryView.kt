package org.ergoplatform.ios.tokens

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.tokens.TokenEntryViewUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * View template for displaying token information on wallet details screen
 */
class WalletDetailsTokenEntryView(
    private val walletToken: WalletToken,
    private val texts: I18NBundle,
    clickListener: () -> Unit
) : UIView(CGRect.Zero()) {

    private val genuineImageContainer = GenuineImageContainer()
    private val thumbnailContainer = ThumbnailContainer(22.0)

    private val valAndName = TokenEntryView().apply {
        addArrangedSubview(genuineImageContainer)
        insertArrangedSubview(thumbnailContainer, 0)
    }
    private val tokenId = Body1Label().apply {
        numberOfLines = 1
        lineBreakMode = NSLineBreakMode.TruncatingMiddle
        textColor = UIColor.secondaryLabel()
    }
    private val balanceLabel = Body1Label().apply {
        textColor = UIColor.secondaryLabel()
        textAlignment = NSTextAlignment.Center
    }

    init {
        layoutMargins = UIEdgeInsets.Zero()

        addSubview(valAndName)
        addSubview(tokenId)
        addSubview(balanceLabel)

        valAndName.topToSuperview(topInset = DEFAULT_MARGIN * 1.5).centerHorizontal(true)
        tokenId.topToBottomOf(valAndName, inset = DEFAULT_MARGIN / 3).widthMatchesSuperview()
        balanceLabel.topToBottomOf(tokenId).widthMatchesSuperview().bottomToSuperview()

        isUserInteractionEnabled = true
        addGestureRecognizer(UITapGestureRecognizer { clickListener() })
    }

    fun bindWalletToken(tokenInformation: TokenInformation? = null): WalletDetailsTokenEntryView {
        displayLogic.bind(tokenInformation)
        return this
    }

    private val displayLogic = object : TokenEntryViewUiLogic(walletToken) {
        override val texts: StringProvider
            get() = IosStringProvider(this@WalletDetailsTokenEntryView.texts)

        override fun setDisplayedTokenName(tokenName: String) {
            valAndName.setTokenName(tokenName)
        }

        override fun setDisplayedTokenId(tokenId: String?) {
            this@WalletDetailsTokenEntryView.tokenId.text = tokenId ?: ""
        }

        override fun setDisplayedBalance(value: String?) {
            valAndName.setTokenAmount(value ?: "")
        }

        override fun setDisplayedPrice(price: String?) {
            balanceLabel.text = price ?: ""
        }

        override fun setGenuineFlag(genuineFlag: Int) {
            genuineImageContainer.setGenuineFlag(genuineFlag)
        }

        override fun setThumbnail(thumbnailType: Int) {
            thumbnailContainer.setThumbnail(thumbnailType)
        }

    }
}