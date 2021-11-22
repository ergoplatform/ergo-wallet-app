package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.Body1BoldLabel
import org.ergoplatform.ios.ui.Body1Label
import org.ergoplatform.ios.ui.DEFAULT_MARGIN
import org.ergoplatform.ios.ui.getAppDelegate
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

        // do not truncate the number, truncate the name instead (default resistance is 750)
        labelTokenVal.setContentCompressionResistancePriority(1000f, UILayoutConstraintAxis.Horizontal)
        // make the name grow, not the number
        labelTokenVal.setContentHuggingPriority(700f, UILayoutConstraintAxis.Horizontal)
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