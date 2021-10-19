package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.coregraphics.CGSize
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UIView

class CardView: UIView(CGRect.Zero()) {
    val contentView = UIView(CGRect.Zero())

    init {
        contentView.apply {
            this.backgroundColor = UIColor.systemGroupedBackground()
            this.layer.setMasksToBounds(true)
            this.layer.cornerRadius = 6.0
        }

        this.setClipsToBounds(false)

        this.addSubview(contentView)
        contentView.widthMatchesSuperview().superViewWrapsHeight()
    }
}