package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.layout.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

abstract class ViewGroupHolder(
    uiView: UIView, treeElement: TreeElement
) : UiViewHolder(uiView, treeElement) {

    abstract fun removeAllChildren()

    abstract fun addSubView(subviewHolder: UiViewHolder)

    abstract fun replaceSubView(oldView: UiViewHolder, newView: UiViewHolder)
}

class BoxViewHolder(
    treeElement: TreeElement
) : ViewGroupHolder(UIView().apply {
    layoutMargins = UIEdgeInsets.Zero()
    if (debugModeColors) backgroundColor = UIColor.green()
}, treeElement) {

    private val box = treeElement.element as Box

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

    override fun removeAllChildren() {
        contentContainer.subviews.forEach { it.removeFromSuperview() }
    }

    override fun addSubView(subviewHolder: UiViewHolder) {
        val viewToAdd = subviewHolder.uiView
        contentContainer.addSubview(viewToAdd)
        configureUiView(subviewHolder)
    }

    private fun configureUiView(viewToAdd: UiViewHolder) {
        val uiView = viewToAdd.uiView

        when (box.getChildHAlignment(viewToAdd.treeElement.element)) {
            HAlignment.START -> uiView.leftToSuperview().rightToSuperview(canBeLess = true)
            HAlignment.CENTER -> uiView.centerHorizontal(true)
            HAlignment.END -> uiView.leftToSuperview(canBeMore = true).rightToSuperview()
            HAlignment.JUSTIFY -> uiView.widthMatchesSuperview()
        }

        when (box.getChildVAlignment(viewToAdd.treeElement.element)) {
            VAlignment.TOP -> uiView.topToSuperview().bottomToSuperview(canBeLess = true)
            VAlignment.CENTER -> uiView.centerVertical().topToSuperview(canBeMore = true)
                .bottomToSuperview(canBeLess = true)
            VAlignment.BOTTOM -> uiView.topToSuperview(canBeMore = true).bottomToSuperview()
        }
    }

    override fun replaceSubView(oldView: UiViewHolder, newView: UiViewHolder) {
        contentContainer.insertSubviewBelow(newView.uiView, oldView.uiView)
        oldView.uiView.removeFromSuperview()
        configureUiView(newView)
    }
}

class StackViewHolder(
    val linearLayout: LinearLayout<*>,
    treeElement: TreeElement
) : ViewGroupHolder(UIStackView().apply {
    axis =
        if (linearLayout is Column) UILayoutConstraintAxis.Vertical
        else UILayoutConstraintAxis.Horizontal
    isLayoutMarginsRelativeArrangement = true
    spacing = linearLayout.spacing.toUiKitSize()
    if (debugModeColors) backgroundColor = UIColor.yellow()
}, treeElement) {

    val uiStackView = uiView as UIStackView
    val weightList = linearLayout.children.map { linearLayout.getChildWeight(it) }
    val weightSum = weightList.sum()
    val allHaveWeights = weightList.all { it > 0 }
    private var firstElementWidthWeight: Pair<UiViewWrapper, Int>? = null

    override fun removeAllChildren() {
        uiStackView.clearArrangedSubviews()
        firstElementWidthWeight = null
    }

    override fun addSubView(subviewHolder: UiViewHolder) {
        // add a wrapper
        val uiViewWrapper = UiViewWrapper()
        uiViewWrapper.layoutMargins = UIEdgeInsets.Zero()
        if (debugModeColors) uiViewWrapper.backgroundColor = if (uiStackView.axis == UILayoutConstraintAxis.Vertical)
            UIColor.orange() else UIColor.red()
        uiStackView.addArrangedSubview(uiViewWrapper)
        uiViewWrapper.addSubview(subviewHolder.uiView)

        configureSubView(subviewHolder)

        val weight = linearLayout.getChildWeight(subviewHolder.treeElement.element)
        if (weight > 0) {
            // configure weight in relation to other elements with a weight
            val firstElementWidthWeight = this.firstElementWidthWeight

            if (firstElementWidthWeight == null) {
                this.firstElementWidthWeight = Pair(uiViewWrapper, weight)
            } else if (uiStackView.axis == UILayoutConstraintAxis.Horizontal) {
                uiViewWrapper.widthMatchesWidthOf(
                    firstElementWidthWeight.first,
                    weight.toDouble() / firstElementWidthWeight.second
                )
            } else if (uiStackView.axis == UILayoutConstraintAxis.Vertical) {
                uiViewWrapper.heightMatchesHeightOf(
                    firstElementWidthWeight.first,
                    weight.toDouble() / firstElementWidthWeight.second
                )
            }
        }
    }

    private fun configureSubView(uiViewHolder: UiViewHolder) {
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
        configureSubView(newView)
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