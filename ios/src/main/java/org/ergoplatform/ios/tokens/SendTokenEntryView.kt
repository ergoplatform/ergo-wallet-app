package org.ergoplatform.ios.tokens

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.TokenAmount
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.toTokenAmount
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * View template for sending token entry on send funds screen
 */
class SendTokenEntryView(
    val uiLogic: SendFundsUiLogic, private val amountErrorField: UIView,
    private val token: WalletToken, ergoToken: ErgoToken,
    texts: I18NBundle
) :
    UIStackView(CGRect.Zero()) {

    private val inputTokenVal: UITextField
    private val maxAmountImageView: UIImageView
    private val labelTokenName = Body1Label()
    private val isSingular: Boolean
    private val amountDelegate = object : OnlyNumericInputTextFieldDelegate() {
        override fun shouldReturn(textField: UITextField?): Boolean {
            resignFirstResponder()
            return true
        }
    }

    init {
        axis = UILayoutConstraintAxis.Horizontal
        spacing = DEFAULT_MARGIN

        val amountChosen = ergoToken.value
        isSingular = token.isSingularToken() && amountChosen == 1L

        inputTokenVal = createTextField().apply {
            delegate = amountDelegate

            if (!isSingular) {
                addOnEditingChangedListener {
                    amountChanged()
                }
            }
        }

        val removeImageView =
            UIImageView(getIosSystemImage(IMAGE_CROSS_CIRCLE, UIImageSymbolScale.Small)).apply {
                tintColor = UIColor.label()
                contentMode = UIViewContentMode.Center
            }

        maxAmountImageView = UIImageView(getIosSystemImage(IMAGE_FULL_AMOUNT, UIImageSymbolScale.Small)).apply {
            tintColor = UIColor.label()
            contentMode = UIViewContentMode.Center

            if (!isSingular) {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    inputTokenVal.text = uiLogic.tokenAmountToText(
                        token.amount!!,
                        token.decimals
                    )
                    amountChanged()
                })
            }
        }

        addArrangedSubview(maxAmountImageView)
        if (!isSingular) {
            addArrangedSubview(inputTokenVal)
        }
        addArrangedSubview(labelTokenName)
        addArrangedSubview(removeImageView)

        maxAmountImageView.enforceKeepIntrinsicWidth()

        inputTokenVal.fixedWidth(80.0)
        inputTokenVal.enforceKeepIntrinsicWidth()

        removeImageView.enforceKeepIntrinsicWidth()
        removeImageView.isUserInteractionEnabled = true
        removeImageView.addGestureRecognizer(UITapGestureRecognizer {
            removeTokenClicked()
        })

        labelTokenName.numberOfLines = 1

        // never shrink or grow the value, but the name
        inputTokenVal.enforceKeepIntrinsicWidth()

        amountDelegate.decimals = token.decimals > 0
        labelTokenName.text = token.name ?: texts.get(STRING_LABEL_UNNAMED_TOKEN)
        inputTokenVal.keyboardType =
            if (token.decimals > 0) UIKeyboardType.NumbersAndPunctuation else UIKeyboardType.NumberPad
        inputTokenVal.text = uiLogic.tokenAmountToText(amountChosen, token.decimals)
        setMaxAmountImageViewVisibility(ergoToken.value)
    }

    private fun setMaxAmountImageViewVisibility(currentRawAmount: Long) {
        maxAmountImageView.alpha = if (isSingular || currentRawAmount == token.amount) 0.0 else 1.0
    }

    private fun removeTokenClicked() {
        if (isSingular || inputTokenVal.text.isBlank()) {
            superview.animateLayoutChanges {
                uiLogic.removeToken(token.tokenId!!)
            }
        } else {
            inputTokenVal.text = ""
            amountChanged()
            inputTokenVal.becomeFirstResponder()
        }
    }

    private fun amountChanged() {
        val amount = getInputAmount()
        uiLogic.setTokenAmount(token.tokenId!!, amount)
        amountErrorField.setHiddenAnimated(true)
        setMaxAmountImageViewVisibility(amount.rawValue)
    }

    private fun getInputAmount(): TokenAmount {
        val amountString = inputTokenVal.text
        return amountString.toTokenAmount(token.decimals) ?: TokenAmount(0, token.decimals)
    }

    fun hasAmount(): Boolean {
        return isSingular || getInputAmount().rawValue > 0
    }

    fun setFocus() {
        if (!isSingular) {
            inputTokenVal.becomeFirstResponder()
        }
    }

}