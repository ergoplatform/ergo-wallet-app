package org.ergoplatform.ios.ui

import org.robovm.apple.uikit.*

class CommonButton: UIButton(UIButtonType.System) {

    init {
        titleLabel?.font = UIFont.getSystemFont(FONT_SIZE_BODY1)
        contentEdgeInsets = UIEdgeInsets(8.0, 8.0, 8.0, 8.0)
        layer.cornerRadius = 18.0
        layer.setMasksToBounds(false)
        setTitleColor(UIColor.label(), UIControlState.Normal)
        setTitleColor(UIColor.secondaryLabel(), UIControlState.Highlighted)
        backgroundColor = UIColor.systemGray4()
    }
}