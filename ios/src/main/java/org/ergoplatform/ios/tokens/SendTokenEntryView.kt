package org.ergoplatform.ios.tokens

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.TokenAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.TokenPrice
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.toTokenAmount
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.uilogic.STRING_LABEL_FIAT_AMOUNT
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.utils.formatTokenPriceToString
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * View template for sending token entry on send funds screen
 */
class SendTokenEntryView(
    val uiLogic: SendFundsUiLogic, private val amountErrorField: UIView,
    private val token: WalletToken, ergoToken: ErgoToken,
    private val texts: I18NBundle,
    private val tokenPrice: TokenPrice?
) : UIView(CGRect.Zero()) {

    private val inputTokenVal: UITextField
    private val labelTokenName = Body1Label()
    private val isSingular: Boolean
    private val amountDelegate = object : OnlyNumericInputTextFieldDelegate() {
        override fun shouldReturn(textField: UITextField?): Boolean {
            resignFirstResponder()
            return true
        }
    }
    private val balanceAmount = Body1BoldLabel()
    private val balanceValue = Body1Label().apply {
        numberOfLines = 1
        textColor = UIColor.secondaryLabel()
        textAlignment = NSTextAlignment.Right
    }

    init {
        layer.cornerRadius = 6.0
        layer.borderWidth = 1.0
        layer.borderColor = UIColor.systemGray().cgColor

        val amountChosen = ergoToken.value
        isSingular = token.isSingularToken() && amountChosen == 1L && tokenPrice == null

        inputTokenVal = UITextField(CGRect.Zero()).apply {
            delegate = amountDelegate

            if (!isSingular) {
                addOnEditingChangedListener {
                    amountChanged()
                }
            }
        }

        val removeImageView =
            UIImageView(getIosSystemImage(IMAGE_REMOVE_TOKEN, UIImageSymbolScale.Small, 20.0)).apply {
                tintColor = UIColor.label()
                contentMode = UIViewContentMode.Center
            }

        addSubview(labelTokenName)
        addSubview(removeImageView)
        labelTokenName.topToSuperview(topInset = DEFAULT_MARGIN).leftToSuperview(inset = DEFAULT_MARGIN)

        if (!isSingular) {
            val maxAmountImageView =
                UIImageView(getIosSystemImage(IMAGE_FULL_AMOUNT, UIImageSymbolScale.Small, 20.0)).apply {
                    tintColor = UIColor.label()
                    contentMode = UIViewContentMode.Center
                    isUserInteractionEnabled = true
                }

            addSubview(maxAmountImageView)
            addSubview(inputTokenVal)
            addSubview(balanceAmount)
            addSubview(balanceValue)
            inputTokenVal.fixedWidth(80.0)
            inputTokenVal.rightToLeftOf(removeImageView, DEFAULT_MARGIN * 2).topToSuperview(topInset = DEFAULT_MARGIN)
                .enforceKeepIntrinsicWidth()
            labelTokenName.rightToLeftOf(inputTokenVal, DEFAULT_MARGIN)
            balanceAmount.topToBottomOf(labelTokenName, DEFAULT_MARGIN).bottomToSuperview(bottomInset = DEFAULT_MARGIN)
                .leftToRightOf(maxAmountImageView, DEFAULT_MARGIN)
            maxAmountImageView.leftToLeftOf(labelTokenName).centerVerticallyTo(balanceAmount)
                .enforceKeepIntrinsicWidth()
            balanceValue.topToTopOf(balanceAmount).rightToRightOf(inputTokenVal)
                .leftToRightOf(balanceAmount, DEFAULT_MARGIN)

            balanceAmount.isUserInteractionEnabled = true
            maxAmountImageView.addGestureRecognizer(UITapGestureRecognizer {
                setMaxAmount()
            })
            balanceAmount.addGestureRecognizer(UITapGestureRecognizer {
                setMaxAmount()
            })
        } else {
            labelTokenName.bottomToSuperview(bottomInset = DEFAULT_MARGIN)
                .rightToLeftOf(removeImageView, DEFAULT_MARGIN * 2)
        }

        removeImageView.centerVertical().rightToSuperview(inset = DEFAULT_MARGIN / 2).enforceKeepIntrinsicWidth()
        removeImageView.isUserInteractionEnabled = true
        removeImageView.addGestureRecognizer(UITapGestureRecognizer {
            removeTokenClicked()
        })

        labelTokenName.numberOfLines = 1

        // never shrink or grow the value, but the name
        inputTokenVal.enforceKeepIntrinsicWidth()
        if (inputTokenVal.superview != null) {
            val underLine = UIView(CGRect.Zero())
            addSubview(underLine)
            underLine.topToBottomOf(inputTokenVal).leftToLeftOf(inputTokenVal)
                .rightToRightOf(inputTokenVal).fixedHeight(1.0).backgroundColor = UIColor.label()
        }

        amountDelegate.decimals = token.decimals > 0
        balanceAmount.text = token.toTokenAmount().toStringPrettified()
        labelTokenName.text = token.name ?: texts.get(STRING_LABEL_UNNAMED_TOKEN)
        inputTokenVal.keyboardType =
            if (token.decimals > 0) UIKeyboardType.NumbersAndPunctuation else UIKeyboardType.NumberPad
        inputTokenVal.text = uiLogic.tokenAmountToText(amountChosen, token.decimals)
        calcBalanceValue(TokenAmount(amountChosen, token.decimals))
    }

    private fun setMaxAmount() {
        inputTokenVal.text = uiLogic.tokenAmountToText(
            token.amount!!,
            token.decimals
        )
        amountChanged()
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
        calcBalanceValue(amount)
    }

    private fun calcBalanceValue(amount: TokenAmount) {
        if (tokenPrice != null) {
            balanceValue.text = texts.format(
                STRING_LABEL_FIAT_AMOUNT,
                formatTokenPriceToString(
                    amount,
                    tokenPrice.ergValue,
                    WalletStateSyncManager.getInstance(),
                    IosStringProvider(texts)
                )
            )
        }
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