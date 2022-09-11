package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.KeyboardType
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.input.DropDownList
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

// TODO alertview with action for less than 5 elements
class DropDownViewHolder(treeElement: TreeElement) :
    UiViewHolder(UIStackView(CGRect.Zero()), treeElement) {

    private val textFieldView = EndIconTextField()
    private val labelView = Body2Label()
    private val mosaikElement = treeElement.element as DropDownList
    private val pickerView = UIPickerView()

    private val entriesList = mosaikElement.entries.map { Pair(it.key, it.value) }

    private var currentSelectedInPicker = -1

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
                    if (currentSelectedInPicker >= 0)
                        pickerView.selectRow(currentSelectedInPicker.toLong(), 0L, false)

                    return super.shouldBeginEditing(textField)
                }
            }

            setCustomActionField(getIosSystemImage(IMAGE_CHEVRON_DOWN, UIImageSymbolScale.Small)!!, action = {
                textFieldView.becomeFirstResponder()
            })
        }

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
            treeElement.changeValueFromInput(entriesList.getOrNull(currentSelectedInPicker)?.first)
            textFieldView.text = treeElement.currentValueAsString
            textFieldView.endEditing(true)
        }
        toolbar.items = NSArray(button)
        toolbar.isUserInteractionEnabled = true
        textFieldView.inputAccessoryView = toolbar
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