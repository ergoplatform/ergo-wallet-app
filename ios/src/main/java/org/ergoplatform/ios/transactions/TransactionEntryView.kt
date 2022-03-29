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

class TransactionEntryView : UIView(CGRect.Zero()) {
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
    private val tokensList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }

    init {
        addSubview(labelErgAmount)
        addSubview(tokensList)
        addSubview(stateLabel)
        addSubview(dateLabel)
        stateLabel.topToSuperview().leftToSuperview().enforceKeepIntrinsicWidth()
        dateLabel.topToSuperview().rightToSuperview().leftToRightOf(stateLabel)
        labelErgAmount.topToBottomOf(stateLabel, DEFAULT_MARGIN).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2) // TODO purpose
        tokensList.topToBottomOf(labelErgAmount, DEFAULT_MARGIN / 2).widthMatchesSuperview(inset = DEFAULT_MARGIN * 2).bottomToSuperview()
    }

    fun bind(txInfo: AddressTransactionWithTokens, tokenClickListener: ((String) -> Unit), texts: I18NBundle) {
        val txHeader = txInfo.addressTransaction
        labelErgAmount.text = texts.format(
            STRING_LABEL_ERG_AMOUNT,
            txHeader.ergAmount.toStringTrimTrailingZeros()
        )
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