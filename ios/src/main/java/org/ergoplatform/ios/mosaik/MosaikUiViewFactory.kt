package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.LabelFormatter
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.Image
import org.ergoplatform.mosaik.model.ui.layout.*
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.mosaik.model.ui.text.StyleableTextLabel
import org.ergoplatform.mosaik.model.ui.text.TruncationType
import org.robovm.apple.foundation.NSData
import org.robovm.apple.uikit.*

object MosaikUiViewFactory {
    fun createUiViewForTreeElement(treeElement: TreeElement): UIView {
        val mosaikViewElement = treeElement.element
        val runtime = treeElement.viewTree.mosaikRuntime

        val uiView = when (mosaikViewElement) {
            is LinearLayout<*> -> {
                UIStackView().apply {
                    axis =
                        if (mosaikViewElement is Column) UILayoutConstraintAxis.Vertical
                        else UILayoutConstraintAxis.Horizontal
                    isLayoutMarginsRelativeArrangement = true
                    spacing = mosaikViewElement.spacing.toUiKitSize()
                }
            }
            is Box -> {
                UIView().apply {
                    layoutMargins = UIEdgeInsets.Zero()
                }
            }
            is Image -> buildMosaikImage(treeElement, mosaikViewElement)
            is StyleableTextLabel<*> -> buildMosaikLabelView(treeElement, mosaikViewElement)
            else -> {
                Body1Label().apply {
                    text = "Unsupported element: ${mosaikViewElement.javaClass.simpleName}"
                }
            }
        }

        if (mosaikViewElement is LayoutElement) {
            val padding = mosaikViewElement.padding.toUiKitSize()
            uiView.layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
        }

        if (treeElement.respondsToClick) {
            uiView.isUserInteractionEnabled = true
            uiView.addGestureRecognizer(UILongPressGestureRecognizer {
                treeElement.longPressed()
            })
            uiView.addGestureRecognizer(UITapGestureRecognizer {
                treeElement.clicked()
            })
        }

        return uiView
    }

    private fun Padding.toUiKitSize() =
        when (this) {
            Padding.NONE -> 0.0
            Padding.QUARTER_DEFAULT -> DEFAULT_MARGIN / 2
            Padding.HALF_DEFAULT -> DEFAULT_MARGIN
            Padding.DEFAULT -> DEFAULT_MARGIN * 2
            Padding.ONE_AND_A_HALF_DEFAULT -> DEFAULT_MARGIN * 3
            Padding.TWICE -> DEFAULT_MARGIN * 4
        }

    private fun buildMosaikImage(treeElement: TreeElement, mosaikViewElement: Image): UIView {
        val size = when (mosaikViewElement.size) {
            Image.Size.SMALL -> 50.0
            Image.Size.MEDIUM -> 100.0
            Image.Size.LARGE -> 200.0
        }
        return UIImageView().apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(size)
            fixedWidth(size)

            treeElement.getResourceBytes?.let { resourceBytesAvailable(this, it) }
        }
    }

    private fun buildMosaikLabelView(treeElement: TreeElement, mosaikViewElement: StyleableTextLabel<*>): UIView {
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

        return labelView
    }

    fun resourceBytesAvailable(uiView: UIView, bytes: ByteArray) {
        // if we have an image that is not set yet
        if (uiView is UIImageView && uiView.image == null) {
            try {
                uiView.image = UIImage(NSData(bytes))
            } catch (t: Throwable) {

            }
        }
    }

}