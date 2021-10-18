package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIFont
import org.robovm.apple.uikit.UILabel

class HeadingLabel: UILabel(CGRect(0.0, 0.0, 100.0, 100.0)) {
    override fun init(p0: CGRect?): Long {
        val init = super.init(p0)
        setupView()
        return init
    }

    override fun init(p0: NSCoder?): Long {
        val init = super.init(p0)
        setupView()
        return init
    }

    private fun setupView() {
        numberOfLines = 1
        textColor = UIColor.black()
        font = UIFont.getSystemFont(18.0)
    }
}