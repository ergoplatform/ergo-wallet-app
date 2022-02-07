package org.ergoplatform.ios.tokens

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * View template for displaying token information on wallet details screen
 */
class DetailTokenEntryView : UIView(CGRect.Zero()) {

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

    init {
        layoutMargins = UIEdgeInsets.Zero()

        addSubview(valAndName)
        addSubview(tokenName)
        addSubview(tokenId)

        valAndName.topToSuperview(topInset = DEFAULT_MARGIN * 1.5).centerHorizontal(true)
        tokenName.topToTopOf(valAndName).widthMatchesSuperview()
        tokenId.topToBottomOf(tokenName).widthMatchesSuperview().bottomToSuperview()
    }

    fun bindWalletToken(walletToken: WalletToken, texts: I18NBundle): DetailTokenEntryView {
        tokenId.text = walletToken.tokenId
        tokenName.text = walletToken.name ?: texts.get(STRING_LABEL_UNNAMED_TOKEN)

        valAndName.isHidden = walletToken.isSingularToken()
        tokenName.isHidden = !walletToken.isSingularToken()

        if (!valAndName.isHidden)
            valAndName.bindWalletToken(walletToken, texts)
        return this
    }
}