package org.ergoplatform.ios.transactions

import org.ergoplatform.ApiServiceManager
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ios.ui.*
import org.ergoplatform.toErgoAmount
import org.ergoplatform.uilogic.STRING_BUTTON_APPLY
import org.ergoplatform.uilogic.STRING_LABEL_FEE_CUSTOM
import org.ergoplatform.uilogic.transactions.SuggestedFee
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * Let the user choose one or more token(s) from a list of tokens
 */
class ChooseFeeAmountViewController(
    val uiLogic: SendFundsViewController.IosSendFundsUiLogic
) : ViewControllerWithKeyboardLayoutGuide() {

    private val texts = getAppDelegate().texts
    private lateinit var chooserStackView: UIStackView
    private lateinit var feeInputField: UITextField

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        val closeButton = addCloseButton()

        chooserStackView = UIStackView().apply {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN
        }

        val feeInputLabel = Body1Label()
        feeInputLabel.text = texts.get(STRING_LABEL_FEE_CUSTOM)

        feeInputField = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Done
            clearButtonMode = UITextFieldViewMode.Always
            keyboardType = UIKeyboardType.NumbersAndPunctuation
            delegate = object : OnlyNumericInputTextFieldDelegate() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    doApply()
                    return true
                }
            }
            text = uiLogic.feeAmount.toStringTrimTrailingZeros()
        }

        val applyButton = TextButton(texts.get(STRING_BUTTON_APPLY))
        applyButton.addOnTouchUpInsideListener { _, _ -> doApply() }

        val containerView = UIView(CGRect.Zero()).apply {
            layoutMargins = UIEdgeInsets(
                0.0,
                DEFAULT_MARGIN * 4,
                0.0,
                DEFAULT_MARGIN * 4
            )
            addSubview(chooserStackView)
            addSubview(feeInputLabel)
            addSubview(feeInputField)
            addSubview(applyButton)

            chooserStackView.topToSuperview(topInset = DEFAULT_MARGIN)
                .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

            feeInputLabel.topToBottomOf(chooserStackView, DEFAULT_MARGIN)
                .leftToLeftOf(chooserStackView)
            feeInputField.topToBottomOf(feeInputLabel)
                .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            applyButton.topToBottomOf(feeInputField, DEFAULT_MARGIN)
                .rightToRightOf(feeInputField)
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN, canBeLess = true)
        }

        val scrollView = containerView.wrapInVerticalScrollView()
        scrollView.setDelaysContentTouches(false)

        view.addSubview(scrollView)
        scrollView.topToBottomOf(closeButton, DEFAULT_MARGIN)
            .widthMatchesSuperview(true)
            .bottomToKeyboard(this)

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        uiLogic.feeSuggestionObserver = { refreshSuggestions() }
        uiLogic.fetchSuggestedFeeData(ApiServiceManager.getOrInit(getAppDelegate().prefs))
        refreshSuggestions()
    }

    override fun viewWillDisappear(animated: Boolean) {
        uiLogic.feeSuggestionObserver = null
        super.viewWillDisappear(animated)
    }

    private fun refreshSuggestions() {
        runOnMainThread {
            val stringProvider = IosStringProvider(getAppDelegate().texts)
            chooserStackView.clearArrangedSubviews()
            uiLogic.suggestedFeeItems.forEach { feeSuggestion ->
                chooserStackView.addArrangedSubview(FeeAmountCard(feeSuggestion, stringProvider).apply {
                    isUserInteractionEnabled = true
                    addGestureRecognizer(UITapGestureRecognizer {
                        setFeeAndDismiss(ErgoAmount(feeSuggestion.feeAmount))
                    })
                })
            }
        }
    }

    private fun doApply() {
        feeInputField.text.toErgoAmount()?.let {
            setFeeAndDismiss(it)
        }
    }

    private fun setFeeAndDismiss(it: ErgoAmount) {
        uiLogic.setNewFeeAmount(it, ApiServiceManager.getOrInit(getAppDelegate().prefs))
        dismissViewController(true) {}
    }

    inner class FeeAmountCard(
        private val feeSuggestion: SuggestedFee,
        private val stringProvider: IosStringProvider
    ) : CardView() {

        private val titleLabel = Body1BoldLabel().apply {
            text = feeSuggestion.getExecutionSpeedText(stringProvider)
            textColor = uiColorErgo
            textAlignment = NSTextAlignment.Center
        }

        private val amountLabel = Body2Label().apply {
            text = feeSuggestion.getFeeAmountText(stringProvider)
            textAlignment = NSTextAlignment.Center
        }

        private val executionSpeedLabel = Body2BoldLabel().apply {
            text = feeSuggestion.getExecutionSpeedText(stringProvider)
            textAlignment = NSTextAlignment.Center
        }

        init {
            contentView.apply {
                addSubview(titleLabel)
                addSubview(amountLabel)
                addSubview(executionSpeedLabel)

                titleLabel.topToSuperview().widthMatchesSuperview()
                amountLabel.topToBottomOf(titleLabel).widthMatchesSuperview()
                executionSpeedLabel.topToBottomOf(amountLabel).widthMatchesSuperview().bottomToSuperview()
            }
        }
    }
}