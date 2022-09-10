package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.LabelFormatter
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.text.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

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
        setTextLabelProperties(mosaikViewElement)

    }

    return UiViewHolder(labelView, treeElement)
}

private fun UILabel.setTextLabelProperties(mosaikViewElement: TextLabel<*>) {
    numberOfLines = mosaikViewElement.maxLines.toLong()
    if (mosaikViewElement.maxLines == 1) lineBreakStrategy = NSLineBreakStrategy.None
    lineBreakMode = if (mosaikViewElement.maxLines > 0)
        mosaikViewElement.truncationType.toUiViewLineBreakMode()
    else
        NSLineBreakMode.WordWrapping

    textAlignment = when (mosaikViewElement.textAlignment) {
        HAlignment.START -> NSTextAlignment.Left
        HAlignment.CENTER -> NSTextAlignment.Center
        HAlignment.END -> NSTextAlignment.Right
        HAlignment.JUSTIFY -> NSTextAlignment.Justified
    }
}

private fun TruncationType.toUiViewLineBreakMode() =
    when (this) {
        TruncationType.START -> NSLineBreakMode.TruncatingHead
        TruncationType.MIDDLE -> NSLineBreakMode.TruncatingMiddle
        TruncationType.END -> NSLineBreakMode.TruncatingTail
    }

class ButtonHolder(treeElement: TreeElement) : UiViewHolder(
    UIView(CGRect.Zero()),
    treeElement
) {
    init {
        uiView.minWidth(100.0)

        val buttonElement = treeElement.element as Button

        val text = buttonElement.text ?: ""

        val uiButton = when (buttonElement.style) {
            Button.ButtonStyle.PRIMARY -> PrimaryButton(text)
            Button.ButtonStyle.SECONDARY -> CommonButton(text)
            Button.ButtonStyle.TEXT -> TextButton(text)
        }.apply {
            val padding = DEFAULT_MARGIN / 2
            layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
        }

        uiButton.titleLabel.setTextLabelProperties(buttonElement)
        uiButton.titleLabel.setAdjustsFontSizeToFitWidth(false)

        uiButton.contentHorizontalAlignment = when (buttonElement.textAlignment) {
            HAlignment.START -> UIControlContentHorizontalAlignment.Left
            HAlignment.CENTER -> UIControlContentHorizontalAlignment.Center
            HAlignment.END -> UIControlContentHorizontalAlignment.Right
            HAlignment.JUSTIFY -> UIControlContentHorizontalAlignment.Center
        }

        uiButton.isEnabled = buttonElement.isEnabled

        uiButton.addOnTouchUpInsideListener { _, _ ->
            treeElement.clicked()
        }

        // the actual button is wrapped in an outer view to have a little padding
        // like on other platforms
        uiView.addSubview(uiButton)
        val padding = DEFAULT_MARGIN / 2
        uiView.layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
        uiButton.edgesToSuperview()
    }

    override val handlesClicks: Boolean get() = true
}
