package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.LabelFormatter
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.Image
import org.ergoplatform.mosaik.model.ui.ViewElement
import org.ergoplatform.mosaik.model.ui.layout.*
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.mosaik.model.ui.text.StyleableTextLabel
import org.ergoplatform.mosaik.model.ui.text.TruncationType
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSData
import org.robovm.apple.uikit.*

const val debugModeColors = false

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
                    if (debugModeColors) backgroundColor = UIColor.yellow()
                }, treeElement)
            }

            is Box -> buildMosaikBox(mosaikViewElement, treeElement)

            is Image -> buildMosaikImage(treeElement, mosaikViewElement)

            is StyleableTextLabel<*> -> buildMosaikLabelView(treeElement, mosaikViewElement)
            else -> {
                UiViewHolder(Body1Label().apply {
                    text = "Unsupported element: ${mosaikViewElement.javaClass.simpleName}"
                }, treeElement)
            }
        }

        val uiView = uiViewHolder.uiView

        if (mosaikViewElement is LayoutElement) {
            val padding = mosaikViewElement.padding.toUiKitSize()
            uiView.layoutMargins = UIEdgeInsets(padding, padding, padding, padding)

            if (padding > 0) {
                uiView.minHeight(padding * 2).minWidth(padding * 2)
            }
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

    private fun buildMosaikBox(
        mosaikViewElement: ViewElement,
        treeElement: TreeElement
    ) = BoxViewHolder(treeElement)

    private fun Padding.toUiKitSize() =
        when (this) {
            Padding.NONE -> 0.0
            Padding.QUARTER_DEFAULT -> DEFAULT_MARGIN * .75
            Padding.HALF_DEFAULT -> DEFAULT_MARGIN * 1.5
            Padding.DEFAULT -> DEFAULT_MARGIN * 3
            Padding.ONE_AND_A_HALF_DEFAULT -> DEFAULT_MARGIN * 4.5
            Padding.TWICE -> DEFAULT_MARGIN * 6
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

        val viewHolder = UiViewHolder(uiImageView, treeElement)

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
            if (debugModeColors) backgroundColor = UIColor.blue()
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

}

open class UiViewHolder(val uiView: UIView, val treeElement: TreeElement) {
    open val contentView: UIView get() = uiView

    open fun removeAllChildren() {
        contentView.subviews.forEach { it.removeFromSuperview() }
    }

    open fun addSubView(subviewHolder: UiViewHolder) {
        val viewToAdd = subviewHolder.uiView
        contentView.addSubview(viewToAdd)
        configureUiView(viewToAdd)
    }

    private fun configureUiView(viewToAdd: UIView) {
        viewToAdd.centerHorizontal(true).topToSuperview().superViewWrapsHeight()
    }

    open fun replaceSubView(oldView: UiViewHolder, newView: UiViewHolder) {
        contentView.insertSubviewBelow(newView.uiView, oldView.uiView)
        oldView.uiView.removeFromSuperview()
        configureUiView(newView.uiView)
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

    open fun onAddedToSuperview() {
        if (isFillMaxWidth()) {
            uiView.widthMatchesSuperview()
        }
    }

    open fun isFillMaxWidth(): Boolean = false
}

class BoxViewHolder(
    treeElement: TreeElement
) : UiViewHolder(UIView().apply {
    layoutMargins = UIEdgeInsets.Zero()
    if (debugModeColors) backgroundColor = UIColor.green()
}, treeElement) {

    private val contentContainer =
        if (treeElement.element is Card)
        // for a card, we have an inner content container
            UIView(CGRect.Zero()).apply {
                uiView.addSubview(this)
                this.layoutMargins = UIEdgeInsets.Zero()
                this.widthMatchesSuperview().superViewWrapsHeight()

                this.layer.setMasksToBounds(true)
                this.layer.cornerRadius = 6.0
                this.layer.borderWidth = 1.0
                this.layer.borderColor = UIColor.systemGray().cgColor
            }
        else
        // for a normal box, outer uiview holds the content as well
            uiView

    override val contentView: UIView
        get() = contentContainer
}

class StackViewHolder(
    val uiStackView: UIStackView,
    treeElement: TreeElement
) : UiViewHolder(uiStackView, treeElement) {

    val linearLayout = treeElement.element as LinearLayout<*>
    val weightList = linearLayout.children.map { linearLayout.getChildWeight(it) }
    val weightSum = weightList.sum()
    val allHaveWeights = weightList.all { it > 0 }

    override fun removeAllChildren() {
        uiStackView.clearArrangedSubviews()
    }

    override fun addSubView(subviewHolder: UiViewHolder) {
        // add a wrapper
        val uiViewWrapper = UiViewWrapper()
        uiViewWrapper.layoutMargins = UIEdgeInsets.Zero()
        if (debugModeColors) uiViewWrapper.backgroundColor = if (uiStackView.axis == UILayoutConstraintAxis.Vertical)
            UIColor.orange() else UIColor.red()
        uiStackView.addArrangedSubview(uiViewWrapper)
        uiViewWrapper.addSubview(subviewHolder.uiView)

        configureView(subviewHolder)

    }

    private fun configureView(uiViewHolder: UiViewHolder) {
        val uiView = uiViewHolder.uiView
        // configure our added view
        val weight = linearLayout.getChildWeight(uiViewHolder.treeElement.element)

        val enforceIntrinsicWidth = !allHaveWeights && weight == 0

        val alignment = linearLayout.getChildAlignment(uiViewHolder.treeElement.element)
        if (alignment is HAlignment) {
            when (alignment) {
                HAlignment.START -> uiView.topToSuperview().bottomToSuperview().leftToSuperview()
                    .rightToSuperview(canBeLess = true)
                HAlignment.CENTER -> uiView.centerHorizontal(true).topToSuperview().bottomToSuperview()
                HAlignment.END -> uiView.topToSuperview().bottomToSuperview().rightToSuperview()
                    .leftToSuperview(canBeMore = true)
                HAlignment.JUSTIFY -> uiView.edgesToSuperview()
            }
        } else if (alignment is VAlignment) {
            when (alignment) {
                VAlignment.TOP -> uiView.topToSuperview().bottomToSuperview(canBeLess = true)
                    .widthMatchesSuperview()
                VAlignment.CENTER -> uiView.centerVertical().widthMatchesSuperview()
                    .topToSuperview(canBeMore = true).bottomToSuperview(canBeLess = true)
                VAlignment.BOTTOM -> uiView.bottomToSuperview().widthMatchesSuperview()
                    .topToSuperview(canBeMore = true)
            }
        }

        if (enforceIntrinsicWidth) {
            uiView.setContentHuggingPriority(1000f, uiStackView.axis)
            uiView.setContentCompressionResistancePriority(1000f, uiStackView.axis)
        } else if (!allHaveWeights) {
            // make the priority lower
            uiView.setContentHuggingPriority(100f, uiStackView.axis)
        }
    }

    override fun replaceSubView(oldView: UiViewHolder, newView: UiViewHolder) {
        // find oldview position
        val wrapper = uiView.subviews.first { it.subviews.firstOrNull() == oldView.uiView }
        wrapper.subviews.forEach { subview ->
            subview.removeFromSuperview()
        }
        wrapper.addSubview(newView.uiView)
        configureView(newView)
    }

    override fun isFillMaxWidth(): Boolean {
        val element = treeElement.element
        return element is Row && !element.isPacked
    }

    private class UiViewWrapper : UIView(CGRect.Zero()) {
        override fun setBounds(v: CGRect) {
            super.setBounds(v)

            // set the preferred max text width to the bounds with to prevent
            // unnecessary text wrapping
            val innerView = subviews.firstOrNull()
            if (innerView is UILabel) {
                innerView.preferredMaxLayoutWidth = v.size.width
            }
        }
    }
}