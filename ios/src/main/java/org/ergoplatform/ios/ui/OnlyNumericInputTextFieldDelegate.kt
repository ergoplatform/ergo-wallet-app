package org.ergoplatform.ios.ui

import org.robovm.apple.foundation.NSRange
import org.robovm.apple.uikit.UITextField
import org.robovm.apple.uikit.UITextFieldDelegateAdapter

open class OnlyNumericInputTextFieldDelegate(val decimals: Boolean = true): UITextFieldDelegateAdapter() {
    override fun shouldChangeCharacters(
        textField: UITextField?,
        range: NSRange?,
        string: String?
    ): Boolean {
        // TODO does not work as intended (allows multiple dots)
        return string?.matches(Regex("^\\d*\\.?(\\d)*$")) ?: true
    }
}