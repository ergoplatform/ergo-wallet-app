package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.KeyboardType
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.input.TextField
import org.robovm.apple.coregraphics.CGRect
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