package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * Textfield supporting end icons
 */
class EndIconTextField: UITextField(CGRect.Zero()) {
    private var customActionImage: UIImage? = null
    private var customAction: Runnable? = null

    init {
        font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        layer.borderWidth = 1.0
        layer.cornerRadius = 4.0
        layer.borderColor = UIColor.systemGray().cgColor
        val padding = UIView(CGRect(0.0, 0.0, 5.0, 10.0))
        leftView = padding
        leftViewMode = UITextFieldViewMode.Always
        fixedHeight(DEFAULT_TEXT_FIELD_HEIGHT)
    }

    fun setHasError(hasError: Boolean) {
        layer.borderColor = (if (hasError) UIColor.systemRed() else UIColor.systemGray()).cgColor
        if (hasError) {
            val errorView = prepareTextFieldImageContainer(
                getIosSystemImage(IMAGE_EXCLAMATION_MARK_FILLED, UIImageSymbolScale.Small)!!,
                UIColor.systemRed()
            )
            rightView = errorView
            rightViewMode = UITextFieldViewMode.Always
        } else if (customActionImage != null) {
            // reset back to former custom actions
            setCustomActionField(customActionImage!!, customAction!!)
        } else {
            rightView = null
        }
    }

    private fun prepareTextFieldImageContainer(image: UIImage, tintColor: UIColor = UIColor.label()): UIView {
        val customIcon = UIImageView(image)
        customIcon.tintColor = tintColor
        customIcon.contentMode = UIViewContentMode.Center
        val iconContainer = UIView(CGRect(0.0, 0.0, 35.0, 30.0))
        iconContainer.addSubview(customIcon)
        return iconContainer
    }

    fun setCustomActionField(image: UIImage, action: Runnable) {
        customAction = action
        customActionImage = image

        val iconContainer = prepareTextFieldImageContainer(image)
        rightView = iconContainer
        rightViewMode = UITextFieldViewMode.Always
        iconContainer.isUserInteractionEnabled = true
        iconContainer.addGestureRecognizer(UITapGestureRecognizer {
            action.run()
        })
    }
}