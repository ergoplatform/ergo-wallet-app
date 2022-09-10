package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.LabelFormatter
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.mosaik.model.ui.text.StyleableTextLabel
import org.ergoplatform.mosaik.model.ui.text.TruncationType
import org.robovm.apple.uikit.NSLineBreakMode
import org.robovm.apple.uikit.NSLineBreakStrategy
import org.robovm.apple.uikit.NSTextAlignment
import org.robovm.apple.uikit.UIColor

fun LabelViewHolder(treeElement: TreeElement, mosaikViewElement: StyleableTextLabel<*>): UiViewHolder {
    val labelView = when (mosaikViewElement.style) {
        LabelStyle.BODY1 -> Body1Label()
        LabelStyle.BODY1BOLD -> Body1BoldLabel()
        LabelStyle.BODY1LINK -> Body1Label() // TODO
        LabelStyle.BODY2 -> Body2Label()
        LabelStyle.BODY2BOLD -> Body2BoldLabel()
        LabelStyle.HEADLINE1 -> Headline1Label()
        LabelStyle.HEADLINE2 -> Headline2Label()
    }

    labelView.apply {
        text = LabelFormatter.getFormattedText(mosaikViewElement, treeElement)
        textColor = when (mosaikViewElement.textColor) {
            ForegroundColor.PRIMARY -> uiColorErgo
            ForegroundColor.DEFAULT -> UIColor.label()
            ForegroundColor.SECONDARY -> UIColor.secondaryLabel()
        }
        if (mosaikViewElement.maxLines > 0) {
            numberOfLines = mosaikViewElement.maxLines.toLong()
            if (mosaikViewElement.maxLines == 1) lineBreakStrategy = NSLineBreakStrategy.None
            lineBreakMode = when (mosaikViewElement.truncationType) {
                TruncationType.START -> NSLineBreakMode.TruncatingHead
                TruncationType.MIDDLE -> NSLineBreakMode.TruncatingMiddle
                TruncationType.END -> NSLineBreakMode.TruncatingTail
            }
        }
        textAlignment = when (mosaikViewElement.textAlignment) {
            HAlignment.START -> NSTextAlignment.Left
            HAlignment.CENTER -> NSTextAlignment.Center
            HAlignment.END -> NSTextAlignment.Right
            HAlignment.JUSTIFY -> NSTextAlignment.Justified
        }
    }

    return UiViewHolder(labelView, treeElement)
}

