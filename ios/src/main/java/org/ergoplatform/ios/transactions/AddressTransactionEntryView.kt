package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.tokens.TokenEntryView
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_LABEL_ERG_AMOUNT
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.uilogic.transactions.getTransactionStateString
import org.ergoplatform.utils.millisecondsToLocalTime
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class AddressTransactionEntryView : UIView(CGRect.Zero()) {
    private val stateLabel = Body1BoldLabel().apply {
        textColor = uiColorErgo
        numberOfLines = 1
    }
    private val dateLabel = Body1Label().apply {
        numberOfLines = 1
        textAlignment = NSTextAlignment.Right
    }
    private val labelErgAmount = Body1BoldLabel().apply {
        numberOfLines = 1
    }
    private val labelPurpose = Body1Label().apply {
        numberOfLines = 3
    }
    private val tokensList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }

    init {
        addSubview(labelErgAmount)
        addSubview(labelPurpose)
        addSubview(tokensList)
        addSubview(stateLabel)
        addSubview(dateLabel)
        stateLabel.topToSuperview().leftToSuperview().enforceKeepIntrinsicWidth()
        dateLabel.topToSuperview().rightToSuperview().leftToRightOf(stateLabel)
        labelErgAmount.topToBottomOf(stateLabel, DEFAULT_MARGIN).leftToSuperview(inset = DEFAULT_MARGIN * 2)
            .enforceKeepIntrinsicWidth()
        labelPurpose.topToTopOf(labelErgAmount).leftToRightOf(labelErgAmount, inset = DEFAULT_MARGIN * 2)
            .rightToSuperview(inset = DEFAULT_MARGIN * 2)
        tokensList.topToBottomOf(labelPurpose, DEFAULT_MARGIN / 2).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)
            .bottomToSuperview()
    }

    fun bind(txInfo: AddressTransactionWithTokens, tokenClickListener: ((String) -> Unit), texts: I18NBundle) {
        val txHeader = txInfo.addressTransaction
        labelErgAmount.text = texts.format(
            STRING_LABEL_ERG_AMOUNT,
            txHeader.ergAmount.toStringTrimTrailingZeros()
        )
        labelPurpose.text = (txHeader.message ?: "").ifBlank { " " } // use at least a space to ensure height > 0
        dateLabel.text = if (txHeader.timestamp > 0) millisecondsToLocalTime(txHeader.timestamp) else ""
        stateLabel.text = txHeader.getTransactionStateString(IosStringProvider(texts))

        tokensList.apply {
            clearArrangedSubviews()

            txInfo.tokens.forEach { token ->
                addArrangedSubview(TokenEntryView().apply {
                    bindWalletToken(
                        token.name.ifBlank { token.tokenId },
                        token.tokenAmount.toStringUsFormatted()
                    )
                    addGestureRecognizer(UITapGestureRecognizer { tokenClickListener(token.tokenId) })
                })
            }
        }
    }
}