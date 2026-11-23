package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGColor
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIView

open class CardView : UIView(CGRect.Zero()) {
    val contentView = UIView(CGRect.Zero())
    val defaultBorderColor: CGColor = UIColor.systemGray().cgColor

    init {
        contentView.apply {
            this.layer.setMasksToBounds(true)
            this.layer.cornerRadius = 6.0
            this.layer.borderWidth = 1.0
            this.layer.borderColor = defaultBorderColor
        }

        this.setClipsToBounds(false)

        this.addSubview(contentView)
        contentView.widthMatchesSuperview().superViewWrapsHeight()
    }

    var cardBorderColor: CGColor?
        get() = contentView.layer.borderColor
        set(value) {
            contentView.layer.borderColor = value
        }
}