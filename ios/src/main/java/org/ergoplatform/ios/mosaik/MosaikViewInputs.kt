package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.ios.addressbook.ChooseAddressDialogViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.WIDTH_ICONS
import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.ui.IconType
import org.ergoplatform.mosaik.model.ui.input.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSRange
import org.robovm.apple.uikit.*

open class TextFieldViewHolder(treeElement: TreeElement) :
    UiViewHolder(UIStackView(CGRect.Zero()), treeElement) {

    protected val textFieldView = EndIconTextField()
    private val labelView = Body2Label()
    private val errorView = Body2Label().apply {
        textColor = uiColorErgo
    }

    protected val stackView = uiView as UIStackView

    private val mosaikElement = treeElement.element as TextField<*>

    private var showsAlternativeText = false
        set(value) {
            field = value
            if (value && mosaikElement.isEnabled && !mosaikElement.isReadOnly) {
                textFieldView.rightViewMode = UITextFieldViewMode.Never
                textFieldView.clearButtonMode = UITextFieldViewMode.Always
            } else {
                textFieldView.clearButtonMode = UITextFieldViewMode.Never
            }
        }

    init {
        val runtime = treeElement.viewTree.mosaikRuntime

        stackView.apply {
            axis = UILayoutConstraintAxis.Vertical
            isLayoutMarginsRelativeArrangement = true
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, 0.0, DEFAULT_MARGIN / 2, 0.0)
            addArrangedSubview(labelView)
            addArrangedSubview(textFieldView)
            addArrangedSubview(errorView)
        }

        textFieldView.apply {
            text = treeElement.currentValueAsString
            isEnabled = mosaikElement.isEnabled
            keyboardType = when (treeElement.keyboardType) {
                KeyboardType.Text -> UIKeyboardType.Default
                KeyboardType.Number -> UIKeyboardType.NumberPad
                KeyboardType.NumberDecimal -> UIKeyboardType.NumbersAndPunctuation
                KeyboardType.Email -> UIKeyboardType.EmailAddress
                KeyboardType.Password -> UIKeyboardType.Default
            }
            isSecureTextEntry = treeElement.keyboardType == KeyboardType.Password
            returnKeyType = when (mosaikElement.imeActionType) {
                TextField.ImeActionType.NEXT -> UIReturnKeyType.Next
                TextField.ImeActionType.DONE -> UIReturnKeyType.Done
                TextField.ImeActionType.GO -> UIReturnKeyType.Go
                TextField.ImeActionType.SEARCH -> UIReturnKeyType.Search
            }

            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldBeginEditing(textField: UITextField?): Boolean {
                    return if (mosaikElement.isReadOnly || showsAlternativeText)
                        false
                    else super.shouldBeginEditing(textField)
                }

                override fun shouldChangeCharacters(
                    textField: UITextField?,
                    range: NSRange?,
                    string: String?
                ): Boolean {
                    return super.shouldChangeCharacters(textField, range, string)
                }

                override fun shouldReturn(textField: UITextField?): Boolean {
                    return if (mosaikElement.onImeAction != null) {
                        runtime.runAction(mosaikElement.onImeAction!!)
                        true
                    } else
                        super.shouldReturn(textField)
                }
            }

            addOnEditingChangedListener {
                if (text.isNullOrEmpty())
                    showsAlternativeText = false

                if (!showsAlternativeText) {
                    setHasError(!treeElement.changeValueFromInput(text.ifEmpty { null }))
                    onChangedText()
                }
            }

            if (mosaikElement.endIcon != null)
                mosaikElement.endIcon?.getUiImage()?.let { endIcon ->
                    setCustomActionField(
                        endIcon,
                        action = {
                            mosaikElement.onEndIconClicked?.let { runtime.runAction(it) }
                        }
                    )
                }
            else if (!mosaikElement.isReadOnly && mosaikElement.isEnabled && mosaikElement is ErgAddressInputField) {
                setCustomActionField(getIosSystemImage(IMAGE_ADDRESSBOOK, UIImageSymbolScale.Small)!!) {
                    mosaikViewController.presentViewController(ChooseAddressDialogViewController { addressWithLabel ->
                        text = addressWithLabel.address
                        sendControlEventsActions(UIControlEvents.EditingChanged)
                    }, true) {}
                }
            }
        }

        labelView.text = mosaikElement.placeholder
        errorView.text = mosaikElement.errorMessage ?: ""
    }

    protected open fun onChangedText() {
        if (LabelFormatter.hasAlternativeText(mosaikElement, treeElement)) {
            val inputText = textFieldView.text
            viewCoroutineScope().launch {
                LabelFormatter.getAlternativeText(mosaikElement, treeElement)?.let {
                    if (inputText == textFieldView.text) runOnMainThread {
                        showsAlternativeText = true
                        textFieldView.text = it
                    }
                }
            }
        }
    }

    override fun onAddedToSuperview() {
        super.onAddedToSuperview()
        onChangedText()
    }

    override fun isFillMaxWidth(): Boolean = true
}

class ErgAmountInputHolder(treeElement: TreeElement) : TextFieldViewHolder(treeElement) {
    private val mosaikRuntime = treeElement.viewTree.mosaikRuntime
    private val hasFiatValue = mosaikRuntime.fiatRate != null
    private val fiatOrErgInputHandler = treeElement.inputValueHandler as FiatOrErgTextInputHandler

    private val secondCurrencyLabel = Body1Label().apply {
        textColor = UIColor.secondaryLabel()
        textAlignment = NSTextAlignment.Right
    }

    private val secondCurrencyContainer = if (fiatOrErgInputHandler.canChangeInputMode())
        secondCurrencyLabel.wrapWithTrailingImage(
            getIosSystemImage(IMAGE_EDIT_CIRCLE, UIImageSymbolScale.Small, 20.0)!!,
            keepWidth = true
        ) else secondCurrencyLabel

    init {
        stackView.addArrangedSubview(secondCurrencyContainer)
        secondCurrencyContainer.isHidden = !hasFiatValue

        if (fiatOrErgInputHandler.canChangeInputMode())
            secondCurrencyContainer.apply {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    fiatOrErgInputHandler.switchInputAmountMode()
                    textFieldView.text = treeElement.currentValueAsString
                    onChangedText()
                })
            }
    }

    override fun onChangedText() {
        super.onChangedText()
        secondCurrencyLabel.text = fiatOrErgInputHandler.getSecondLabelString((treeElement.currentValue as? Long) ?: 0)
    }
}

class DropDownViewHolder(treeElement: TreeElement) :
    UiViewHolder(UIStackView(CGRect.Zero()), treeElement) {

    private val textFieldView = EndIconTextField()
    private val labelView = Body2Label()
    private val mosaikElement = treeElement.element as DropDownList
    private val pickerView = UIPickerView()

    private val entriesList = mosaikElement.entries.map { Pair(it.key, it.value) }

    private var currentSelectedInPicker = -1
    private val isActionController = entriesList.size < 5

    init {
        val stackView = uiView as UIStackView

        stackView.apply {
            axis = UILayoutConstraintAxis.Vertical
            isLayoutMarginsRelativeArrangement = true
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, 0.0, DEFAULT_MARGIN / 2, 0.0)
            addArrangedSubview(labelView)
            addArrangedSubview(textFieldView)
        }

        textFieldView.apply {
            text = treeElement.currentValueAsString
            isEnabled = mosaikElement.isEnabled
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldChangeCharacters(
                    textField: UITextField?,
                    range: NSRange?,
                    string: String?
                ): Boolean = false

                override fun shouldBeginEditing(textField: UITextField?): Boolean {
                    return if (isActionController) {
                        val mosaikRuntime = treeElement.viewTree.mosaikRuntime as MosaikViewController.IosMosaikRuntime
                        val uac = UIAlertController(
                            mosaikRuntime.formatString(StringConstant.PleaseChoose),
                            "", UIAlertControllerStyle.ActionSheet
                        )
                        entriesList.forEachIndexed { idx, (_, title) ->
                            uac.addAction(UIAlertAction(title, UIAlertActionStyle.Default) {
                                currentSelectedInPicker = idx
                                elementSelected()
                            })
                        }

                        uac.popoverPresentationController?.sourceView = textFieldView
                        mosaikRuntime.viewController.presentViewController(uac, true) {}

                        false
                    } else {
                        if (currentSelectedInPicker >= 0)
                            pickerView.selectRow(currentSelectedInPicker.toLong(), 0L, false)

                        super.shouldBeginEditing(textField)
                    }
                }
            }

            setCustomActionField(getIosSystemImage(IMAGE_CHEVRON_DOWN, UIImageSymbolScale.Small)!!, action = {
                textFieldView.becomeFirstResponder()
            })
        }

        if (!isActionController)
            setupPicker()
    }

    private fun setupPicker() {
        textFieldView.inputView = pickerView.apply {
            delegate = DropDownPicker()
            dataSource = DropDownDataSource()
            val currentValue = treeElement.currentValue
            currentSelectedInPicker = entriesList.indexOfFirst { it.first == currentValue }
        }

        val toolbar = UIToolbar()
        toolbar.sizeToFit()
        val button = UIBarButtonItem(UIBarButtonSystemItem.Done)
        button.setOnClickListener {
            elementSelected()
            textFieldView.endEditing(true)
        }
        toolbar.items = NSArray(button)
        toolbar.isUserInteractionEnabled = true
        textFieldView.inputAccessoryView = toolbar
    }

    private fun elementSelected() {
        treeElement.changeValueFromInput(entriesList.getOrNull(currentSelectedInPicker)?.first)
        textFieldView.text = treeElement.currentValueAsString
    }

    override fun isFillMaxWidth(): Boolean = true

    inner class DropDownPicker : UIPickerViewDelegateAdapter() {
        override fun getRowTitle(pickerView: UIPickerView?, row: Long, component: Long): String {
            return entriesList[row.toInt()].second
        }

        override fun didSelectRow(pickerView: UIPickerView?, row: Long, component: Long) {
            if (textFieldView.isEditing) {
                // somehow callback is firing after editing ended
                currentSelectedInPicker = row.toInt()
            }
        }
    }

    inner class DropDownDataSource : UIPickerViewDataSourceAdapter() {
        override fun getNumberOfComponents(pickerView: UIPickerView?): Long = 1

        override fun getNumberOfRows(pickerView: UIPickerView?, component: Long): Long {
            return entriesList.size.toLong()
        }
    }
}

class CheckboxViewHolder(treeElement: TreeElement) :
    UiViewHolder(UIView(CGRect.Zero()), treeElement) {

    init {
        val mosaikElement = treeElement.element as CheckboxLabel

        // build the label using the normal holder
        val labelView = LabelViewHolder(treeElement, mosaikElement).uiView as ThemedLabel
        val confirmationCheck = UISwitch(CGRect.Zero()).apply {
            isOn = mosaikElement.value == true
            addOnValueChangedListener {
                treeElement.valueChanged(isOn)
            }
        }

        uiView.addSubview(labelView)
        uiView.addSubview(confirmationCheck)
        labelView.rightToSuperview().bottomToSuperview().topToSuperview()
        confirmationCheck.leftToSuperview().centerVertical().rightToLeftOf(labelView, DEFAULT_MARGIN * 2)
            .fixedWidth(50.0)
        uiView.minHeight(50.0)
        uiView.layoutMargins = UIEdgeInsets.Zero()
    }

}

class InputButtonHolder(treeElement: TreeElement) :
    UiViewHolder(UIView(CGRect.Zero()), treeElement) {

    private val mosaikElement = treeElement.element as StyleableInputButton<*>
    private val isButtonStyle = when (mosaikElement.style) {
        StyleableInputButton.InputButtonStyle.BUTTON_PRIMARY,
        StyleableInputButton.InputButtonStyle.BUTTON_SECONDARY -> true
        StyleableInputButton.InputButtonStyle.ICON_PRIMARY,
        StyleableInputButton.InputButtonStyle.ICON_SECONDARY -> false
    }

    private var valueWatchJob: Job? = null
    private var lastWatchedValue: Any? = null

    init {

        if (isButtonStyle) {
            uiView.minWidth(100.0)
            uiView.maxWidth(200.0)

            val uiButton = when (mosaikElement.style) {
                StyleableInputButton.InputButtonStyle.BUTTON_PRIMARY -> PrimaryButton("")
                StyleableInputButton.InputButtonStyle.BUTTON_SECONDARY -> CommonButton("")
                StyleableInputButton.InputButtonStyle.ICON_PRIMARY,
                StyleableInputButton.InputButtonStyle.ICON_SECONDARY -> throw IllegalStateException()
            }.apply {
                val padding = DEFAULT_MARGIN / 2
                layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
                titleLabel.setAdjustsFontSizeToFitWidth(false)
                titleLabel.lineBreakMode = NSLineBreakMode.TruncatingTail
                isEnabled = mosaikElement.isEnabled
                addOnTouchUpInsideListener { _, _ ->
                    mosaikViewController.lastViewInteracted = this
                    treeElement.clicked()
                }
            }

            // the actual button is wrapped in an outer view to have a little padding
            // like on other platforms
            uiView.addSubview(uiButton)
            val padding = DEFAULT_MARGIN / 2
            uiView.layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
            uiButton.edgesToSuperview()

            // we don't use viewtree's registerJobFor here as it is meant to be used for
            // completable jobs only, while the observer here runs as long as the element
            // exists
            valueWatchJob =
                viewCoroutineScope().launch {
                    treeElement.viewTree.valueState.collectLatest {
                        val currentValue = treeElement.currentValue

                        if (currentValue == null || currentValue != lastWatchedValue) {
                            runOnMainThread {
                                uiButton.setTitle(treeElement.currentValueAsString, UIControlState.Normal)
                            }
                            lastWatchedValue = currentValue
                        }
                    }
                }
        } else {
            val uiImageView = UIImageView().apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                fixedHeight(WIDTH_ICONS)
                fixedWidth(WIDTH_ICONS)
                enforceKeepIntrinsicWidth()
                tintColor =
                    if (!mosaikElement.isEnabled)
                        UIColor.secondaryLabel()
                    else if (mosaikElement.style == StyleableInputButton.InputButtonStyle.ICON_PRIMARY)
                        uiColorErgo
                    else
                        UIColor.label()

                image = IconType.WALLET.getUiImage()
            }
            uiView.addSubview(uiImageView)
            uiImageView.edgesToSuperview()
        }
    }

    override fun onRemovedFromSuperview() {
        super.onRemovedFromSuperview()
        valueWatchJob?.cancel()
    }

    override val handlesClicks: Boolean get() = isButtonStyle
}