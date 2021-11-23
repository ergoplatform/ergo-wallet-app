package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.STRING_LABEL_MORE_TOKENS
import org.ergoplatform.utils.formatTokenAmounts
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView

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

    fun bindWalletToken(walletToken: WalletToken): TokenEntryView {
        labelTokenName.text = walletToken.name
        labelTokenVal.text =
            formatTokenAmounts(
                walletToken.amount ?: 0,
                walletToken.decimals,
            )

        return this
    }

    fun bindHasMoreTokenHint(moreTokenNum: Int): TokenEntryView {
        labelTokenVal.text = "+$moreTokenNum"
        labelTokenName.text = getAppDelegate().texts.get(STRING_LABEL_MORE_TOKENS)
        return this
    }
}