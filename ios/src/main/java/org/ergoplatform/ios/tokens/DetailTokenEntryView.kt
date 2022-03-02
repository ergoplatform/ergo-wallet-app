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
class DetailTokenEntryView(private val walletToken: WalletToken, private val texts: I18NBundle) :
    UIView(CGRect.Zero()) {

    private val valAndName = TokenEntryView()
    private val tokenName = Body1Label().apply {
        textAlignment = NSTextAlignment.Center
        numberOfLines = 1
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
        addSubview(tokenName)
        addSubview(tokenId)
        addSubview(balanceLabel)

        valAndName.topToSuperview(topInset = DEFAULT_MARGIN * 1.5).centerHorizontal(true)
        tokenName.topToTopOf(valAndName).widthMatchesSuperview()
        tokenId.topToBottomOf(tokenName, inset = DEFAULT_MARGIN / 2).widthMatchesSuperview()
        balanceLabel.topToBottomOf(tokenId).widthMatchesSuperview().bottomToSuperview()
    }

    fun bindWalletToken(tokenInformation: TokenInformation? = null): DetailTokenEntryView {
        displayLogic.bind(tokenInformation)
        return this
    }

    private val displayLogic = object : TokenEntryViewUiLogic(walletToken) {
        override val texts: StringProvider
            get() = IosStringProvider(this@DetailTokenEntryView.texts)

        override fun setDisplayedTokenName(tokenName: String) {
            this@DetailTokenEntryView.tokenName.text = tokenName
            valAndName.setTokenName(tokenName)
        }

        override fun setDisplayedTokenId(tokenId: String?) {
            this@DetailTokenEntryView.tokenId.text = tokenId ?: ""
        }

        override fun setDisplayedBalance(value: String?) {
            valAndName.isHidden = value == null
            tokenName.isHidden = value != null
            value?.let { valAndName.setTokenAmount(value) }
        }

        override fun setDisplayedPrice(price: String?) {
            balanceLabel.text = price ?: ""
        }

        override fun setGenuineFlag(genuineFlag: Int) {
            // TODO("Not yet implemented")
        }

        override fun setThumbnail(thumbnailType: Int) {
            // TODO("Not yet implemented")
        }

    }
}