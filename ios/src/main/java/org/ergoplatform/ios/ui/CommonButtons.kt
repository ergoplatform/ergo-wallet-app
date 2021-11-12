package org.ergoplatform.ios.ui

import org.robovm.apple.uikit.*

open class CommonButton(caption: String) : TextButton(caption, false) {

    init {
        contentEdgeInsets = UIEdgeInsets(8.0, 8.0, 8.0, 8.0)
        layer.cornerRadius = 18.0
        layer.setMasksToBounds(false)

        setTitleColor(UIColor.label(), UIControlState.Normal)
        backgroundColor = UIColor.systemGray4()
    }
}

open class TextButton(caption: String, bold: Boolean = true) : UIButton(UIButtonType.System) {
    init {
        titleLabel?.font =
            UIFont.getSystemFont(FONT_SIZE_BODY1, if (bold) UIFontWeight.Semibold else UIFontWeight.Regular)
        setTitleColor(uiColorErgo, UIControlState.Normal)
        setTitleColor(UIColor.secondaryLabel(), UIControlState.Highlighted)
        setTitleColor(UIColor.systemGray(), UIControlState.Disabled)
        setTitle(
            caption,
            UIControlState.Normal
        )
    }
}

class PrimaryButton(caption: String) : CommonButton(caption) {

    init {
        backgroundColor = uiColorErgo
    }
}