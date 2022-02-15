package org.ergoplatform.ios.tokens

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_LABEL_MORE_TOKENS
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.utils.formatTokenAmounts
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView

/**
 * View template for displaying token information on wallet overview and wallet details screen
 */
class TokenEntryView : UIStackView(CGRect.Zero()) {

    private val labelTokenVal = Body1BoldLabel()
    private val labelTokenName = Body1Label()

    init {
        axis = UILayoutConstraintAxis.Horizontal
        spacing = DEFAULT_MARGIN

        addArrangedSubview(labelTokenVal)
        addArrangedSubview(labelTokenName)

        labelTokenVal.numberOfLines = 1
        labelTokenName.numberOfLines = 1

        // never shrink or grow the value, but the name
        labelTokenVal.enforceKeepIntrinsicWidth()
    }

    fun bindWalletToken(walletToken: WalletToken, texts: I18NBundle): TokenEntryView {
        labelTokenName.text = walletToken.name ?: texts.get(STRING_LABEL_UNNAMED_TOKEN)
        labelTokenVal.text =
            formatTokenAmounts(
                walletToken.amount ?: 0,
                walletToken.decimals,
            )

        return this
    }

    fun bindWalletToken(tokenName: String?, amount: String?) {
        labelTokenName.text = tokenName ?: ""
        labelTokenVal.text = amount ?: ""
    }

    fun bindHasMoreTokenHint(moreTokenNum: Int): TokenEntryView {
        labelTokenVal.text = "+$moreTokenNum"
        labelTokenName.text = getAppDelegate().texts.get(STRING_LABEL_MORE_TOKENS)
        return this
    }
}