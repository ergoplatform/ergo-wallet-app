package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.WIDTH_ICONS
import org.ergoplatform.mosaik.KeyboardType
import org.ergoplatform.mosaik.StringConstant
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.IconType
import org.ergoplatform.mosaik.model.ui.input.CheckboxLabel
import org.ergoplatform.mosaik.model.ui.input.DropDownList
import org.ergoplatform.mosaik.model.ui.input.StyleableInputButton
import org.ergoplatform.mosaik.model.ui.input.TextField
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSRange
import org.robovm.apple.uikit.*

class TextFieldViewHolder(treeElement: TreeElement) :
    UiViewHolder(UIStackView(CGRect.Zero()), treeElement) {

    private val textFieldView = EndIconTextField()
    private val labelView = Body2Label()
    private val errorView = Body2Label().apply {
        textColor = uiColorErgo
    }

    init {
        val stackView = uiView as UIStackView
        val mosaikElement = treeElement.element as TextField<*>
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
                    return if (mosaikElement.isReadOnly)
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
                setHasError(!treeElement.changeValueFromInput(text.ifEmpty { null }))
            }

            mosaikElement.endIcon?.getUiImage()?.let { endIcon ->
                setCustomActionField(
                    endIcon,
                    action = {
                        mosaikElement.onEndIconClicked?.let { runtime.runAction(it) }
                    }
                )
            }
        }

        labelView.text = mosaikElement.placeholder
        errorView.text = mosaikElement.errorMessage ?: ""
    }

    override fun isFillMaxWidth(): Boolean = true
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
                        entriesList.forEachIndexed { idx, (key, title) ->
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
                (treeElement.viewTree.mosaikRuntime as MosaikViewController.IosMosaikRuntime).viewController.viewControllerScope.launch {
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