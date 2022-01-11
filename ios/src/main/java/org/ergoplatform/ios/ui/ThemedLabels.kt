package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIFont
import org.robovm.apple.uikit.UIFontWeight
import org.robovm.apple.uikit.UILabel

abstract class ThemedLabel() : UILabel(CGRect.Zero()) {
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
        numberOfLines = 0
        textColor = UIColor.label()
        font = UIFont.getSystemFont(getFontSize(), if (isBold()) UIFontWeight.Semibold else UIFontWeight.Regular)
    }

    protected abstract fun getFontSize(): Double

    open fun isBold(): Boolean = false
}

open class Body1Label() : ThemedLabel() {
    override fun getFontSize() = FONT_SIZE_BODY1
}

class Body1BoldLabel() : Body1Label() {
    override fun isBold(): Boolean {
        return true
    }
}

open class Body2Label() : ThemedLabel() {
    override fun getFontSize() = 16.0
}

class Body2BoldLabel() : Body2Label() {
    override fun isBold(): Boolean {
        return true
    }
}

class Headline1Label() : ThemedLabel() {
    override fun getFontSize() = FONT_SIZE_HEADLINE1

    override fun isBold(): Boolean {
        return true
    }
}

class Headline2Label() : ThemedLabel() {
    override fun getFontSize() = FONT_SIZE_HEADLINE2
    override fun isBold(): Boolean {
        return true
    }
}

