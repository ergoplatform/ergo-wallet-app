package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.ios.tokens.TokenEntryView
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class TransactionBoxEntryView : UIView(CGRect.Zero()) {
    private val labelBoxAddress = Body1BoldLabel().apply {
        numberOfLines = 1
        textColor = uiColorErgo
        lineBreakMode = NSLineBreakMode.TruncatingMiddle
        isUserInteractionEnabled = true
        addGestureRecognizer(UITapGestureRecognizer {
            numberOfLines = if (numberOfLines == 1L) 10 else 1
        })
    }
    private val labelErgAmount = Body1BoldLabel().apply {
        numberOfLines = 1
    }
    private val tokensList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }

    init {
        layoutMargins = UIEdgeInsets.Zero()

        addSubview(labelBoxAddress)
        addSubview(labelErgAmount)
        addSubview(tokensList)

        labelBoxAddress.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview(inset = DEFAULT_MARGIN)
        labelErgAmount.topToBottomOf(labelBoxAddress).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)
        tokensList.topToBottomOf(labelErgAmount).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2).bottomToSuperview()
    }

    fun bindBoxView(
        value: Long?,
        address: String,
        assets: List<AssetInstanceInfo>?,
        texts: I18NBundle
    ): TransactionBoxEntryView {
        labelErgAmount.text = value?.let {
            if (it != 0L) texts.format(
                STRING_LABEL_ERG_AMOUNT,
                ErgoAmount(value).toStringTrimTrailingZeros()
            ) else null
        } ?: ""
        labelBoxAddress.text = address

        tokensList.apply {
            clearArrangedSubviews()

            assets?.forEach {
                addArrangedSubview(TokenEntryView().apply {
                    // we use the token id here, we don't have the name in the cold wallet context
                    bindWalletToken(
                        it.name ?: it.tokenId,
                        TokenAmount(it.amount, it.decimals ?: 0).toStringTrimTrailingZeros()
                    )
                })
            }
        }

        return this
    }
}