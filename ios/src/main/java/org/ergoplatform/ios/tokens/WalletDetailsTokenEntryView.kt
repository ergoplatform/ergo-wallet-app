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
class WalletDetailsTokenEntryView(private val walletToken: WalletToken, private val texts: I18NBundle) :
    UIView(CGRect.Zero()) {

    private val genuineImage = UIImageView(CGRect.Zero()).apply {
        tintColor = uiColorErgo
        contentMode = UIViewContentMode.ScaleAspectFit
        fixedWidth(20.0)
    }
    private val genuineImageContainer = UIView(CGRect.Zero()).apply {
        addSubview(genuineImage)
        genuineImage.edgesToSuperview()
        layoutMargins = UIEdgeInsets(0.0, DEFAULT_MARGIN, 0.0, 0.0)
    }
    private val thumbnailPicture = UIImageView(CGRect.Zero()).apply {
        contentMode = UIViewContentMode.ScaleAspectFit
        tintColor = UIColor.systemBackground()
        fixedWidth(16.0)
        fixedHeight(16.0)
    }
    private val thumbnailContainer = UIView(CGRect.Zero()).apply {
        val backGround = UIImageView(octagonImage.imageWithTintColor(UIColor.secondaryLabel())).apply {
            fixedWidth(24.0)
            fixedHeight(24.0)
            contentMode = UIViewContentMode.ScaleAspectFit
        }
        addSubview(backGround)
        backGround.edgesToSuperview()
        addSubview(thumbnailPicture)
        thumbnailPicture.centerVertical().centerHorizontallyTo(backGround)
        layoutMargins = UIEdgeInsets(0.0, 0.0, 0.0, DEFAULT_MARGIN)
    }

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
            val imageName = getTokenGenuineImageName(genuineFlag)
            genuineImage.image = imageName?.let { getIosSystemImage(it, UIImageSymbolScale.Small, 20.0) }
            genuineImageContainer.isHidden = imageName == null
        }

        override fun setThumbnail(thumbnailType: Int) {
            val imageName = getTokenThumbnailImageName(thumbnailType)
            thumbnailPicture.image = imageName?.let {
                getIosSystemImage(it, UIImageSymbolScale.Small, 13.0)
            }
            thumbnailContainer.isHidden = imageName == null
        }

    }
}