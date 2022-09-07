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
    fun createUiViewForTreeElement(treeElement: TreeElement): UiViewHolder {
        val mosaikViewElement = treeElement.element
        val runtime = treeElement.viewTree.mosaikRuntime

        val uiViewHolder = when (mosaikViewElement) {
            is LinearLayout<*> -> {
                StackViewHolder(UIStackView().apply {
                    axis =
                        if (mosaikViewElement is Column) UILayoutConstraintAxis.Vertical
                        else UILayoutConstraintAxis.Horizontal
                    isLayoutMarginsRelativeArrangement = true
                    spacing = mosaikViewElement.spacing.toUiKitSize()
                })
            }
            is Box -> {
                UiViewHolder(UIView().apply {
                    layoutMargins = UIEdgeInsets.Zero()
                })
            }
            is Image -> buildMosaikImage(treeElement, mosaikViewElement)
            is StyleableTextLabel<*> -> buildMosaikLabelView(treeElement, mosaikViewElement)
            else -> {
                UiViewHolder(Body1Label().apply {
                    text = "Unsupported element: ${mosaikViewElement.javaClass.simpleName}"
                })
            }
        }

        val uiView = uiViewHolder.uiView

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

        return uiViewHolder
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

    private fun buildMosaikImage(treeElement: TreeElement, mosaikViewElement: Image): UiViewHolder {
        val size = when (mosaikViewElement.size) {
            Image.Size.SMALL -> 50.0
            Image.Size.MEDIUM -> 100.0
            Image.Size.LARGE -> 200.0
        }
        val uiImageView = UIImageView().apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(size)
            fixedWidth(size)
        }

        val viewHolder = UiViewHolder(uiImageView)

        treeElement.getResourceBytes?.let { viewHolder.resourceBytesAvailable(it) }

        return viewHolder
    }

    private fun buildMosaikLabelView(treeElement: TreeElement, mosaikViewElement: StyleableTextLabel<*>): UiViewHolder {
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

        return UiViewHolder(labelView)
    }

}

open class UiViewHolder(val uiView: UIView) {
    open fun removeAllChildren() {
        uiView.subviews.forEach { it.removeFromSuperview() }
    }

    open fun addSubView(subviewHolder: UiViewHolder) {
        val viewToAdd = subviewHolder.uiView
        uiView.addSubview(viewToAdd)
        viewToAdd.centerHorizontal(true).topToSuperview().superViewWrapsHeight()
    }

    fun resourceBytesAvailable(bytes: ByteArray) {
        // if we have an image that is not set yet
        if (uiView is UIImageView && uiView.image == null) {
            try {
                uiView.image = UIImage(NSData(bytes))
            } catch (t: Throwable) {

            }
        }
    }
}

class StackViewHolder(val uiStackView: UIStackView) : UiViewHolder(uiStackView) {
    override fun removeAllChildren() {
        uiStackView.clearArrangedSubviews()
    }

    override fun addSubView(subviewHolder: UiViewHolder) {
        // TODO different layouts, this is always JUSTIFY without weight
        uiStackView.addArrangedSubview(subviewHolder.uiView)
    }
}