package org.ergoplatform.ios.ui

import org.ergoplatform.ErgoAmount
import org.ergoplatform.ergoCurrencyText
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ErgoAmountView(bold: Boolean, baseSize: Double = FONT_SIZE_HEADLINE2, private val numDecimals: Int = 4) :
    UIStackView(CGRect.Zero()) {

    private val integerValueLabel = object : ThemedLabel() {
        override fun getFontSize(): Double {
            return baseSize
        }

        override fun isBold(): Boolean {
            return bold
        }
    }

    private val decimalValueLabel = object : ThemedLabel() {
        override fun getFontSize(): Double {
            return baseSize * .6
        }

        override fun isBold(): Boolean {
            return bold
        }
    }
    private val currencyLabel = object : ThemedLabel() {
        override fun getFontSize(): Double {
            return baseSize
        }

        override fun isBold(): Boolean {
            return bold
        }

    }

    init {
        axis = UILayoutConstraintAxis.Horizontal
        alignment = UIStackViewAlignment.Top

        val decimalContainer = UIView()
        decimalContainer.layoutMargins = UIEdgeInsets(baseSize * .1f, 0.0, 0.0, 0.0)
        decimalContainer.addSubview(decimalValueLabel)
        decimalValueLabel.leftToSuperview().rightToSuperview().bottomToSuperview().topToSuperview()

        addArrangedSubview(integerValueLabel)
        addArrangedSubview(decimalContainer)
        addArrangedSubview(currencyLabel)

        setCustomSpacing(baseSize * 0.15, decimalContainer)

        integerValueLabel.numberOfLines = 1
        decimalValueLabel.numberOfLines = 1
        currencyLabel.numberOfLines = 1

        integerValueLabel.enforceKeepIntrinsicWidth()
        // this compression resistance should be slightly lower, so don't use enforceKeepIntrinsicWidth
        decimalValueLabel.setContentCompressionResistancePriority(900f, UILayoutConstraintAxis.Horizontal)
        decimalValueLabel.setContentHuggingPriority(700f, UILayoutConstraintAxis.Horizontal)

        currencyLabel.text = ergoCurrencyText

        setErgoAmount(ErgoAmount.ZERO)
    }

    fun setErgoAmount(amount: ErgoAmount) {
        val amountString = amount.toStringRoundToDecimals(numDecimals)
        decimalValueLabel.text = amountString.takeLast(numDecimals)
        integerValueLabel.text = amountString.dropLast(numDecimals).trimEnd('.')
    }
}