package org.ergoplatform.ios.tokens

import org.ergoplatform.TokenAmount
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.toTokenAmount
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * View template for sending token entry on send funds screen
 */
class SendTokenEntryView(val uiLogic: SendFundsUiLogic, private val amountErrorField: UIView) :
    UIStackView(CGRect.Zero()) {

    private val inputTokenVal: UITextField
    private val labelTokenName = Body1Label()
    private var token: WalletToken? = null
    private val amountDelegate = object : OnlyNumericInputTextFieldDelegate() {
        override fun shouldReturn(textField: UITextField?): Boolean {
            resignFirstResponder()
            return true
        }
    }

    init {
        axis = UILayoutConstraintAxis.Horizontal
        spacing = DEFAULT_MARGIN

        inputTokenVal = createTextField().apply {
            delegate = amountDelegate

            addOnEditingChangedListener {
                amountChanged()
            }
        }

        val removeImageView =
            UIImageView(getIosSystemImage(IMAGE_CROSS_CIRCLE, UIImageSymbolScale.Small)).apply {
                tintColor = UIColor.label()
                contentMode = UIViewContentMode.Center
            }

        val maxAmountImageView = UIImageView(getIosSystemImage(IMAGE_FULL_AMOUNT, UIImageSymbolScale.Small)).apply {
            tintColor = UIColor.label()
            contentMode = UIViewContentMode.Center
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                token?.let { token ->
                    inputTokenVal.text = uiLogic.tokenAmountToText(
                        token.amount!!,
                        token.decimals
                    )
                    amountChanged()
                }
            })
        }

        addArrangedSubview(maxAmountImageView)
        addArrangedSubview(inputTokenVal)
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
    }

    private fun removeTokenClicked() {
        token?.let {
            if (inputTokenVal.text.isBlank()) {
                superview.animateLayoutChanges {
                    uiLogic.removeToken(it.tokenId!!)
                }
            } else {
                inputTokenVal.text = ""
                amountChanged()
            }
        }
    }

    private fun amountChanged() {
        token?.let { token ->
            val amount = getInputAmount()
            uiLogic.setTokenAmount(token.tokenId!!, amount)
            amountErrorField.setHiddenAnimated(true)
        }
    }

    private fun getInputAmount(): TokenAmount {
        val amountString = inputTokenVal.text
        val amount =
            amountString.toTokenAmount(token!!.decimals) ?: TokenAmount(0, token!!.decimals)
        return amount
    }

    fun hasAmount(): Boolean {
        return token?.let { getInputAmount().rawValue > 0 } ?: false
    }

    fun setFocus() {
        inputTokenVal.becomeFirstResponder()
    }

    fun bindWalletToken(walletToken: WalletToken, ergoToken: ErgoToken): SendTokenEntryView {
        token = walletToken
        amountDelegate.decimals = walletToken.decimals > 0
        labelTokenName.text = walletToken.name
        inputTokenVal.keyboardType =
            if (walletToken.decimals > 0) UIKeyboardType.NumbersAndPunctuation else UIKeyboardType.NumberPad
        inputTokenVal.text = uiLogic.tokenAmountToText(ergoToken.value, walletToken.decimals)
        return this
    }
}