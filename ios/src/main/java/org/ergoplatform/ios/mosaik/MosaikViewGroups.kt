package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.MosaikViewGroupHolder
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.layout.*
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*
import kotlin.math.max

class BoxViewHolder(
    treeElement: TreeElement
) : UiViewHolder(UIView().apply {
    layoutMargins = UIEdgeInsets.Zero()
    if (debugModeColors) backgroundColor = UIColor.green()
}, treeElement), MosaikViewGroupHolder<UiViewHolder> {

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

        // try to pack the box around the inner views whenever possible
        uiView.edgesToSuperview(priority = 100f)
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
) : UiViewHolder(UIStackView().apply {
    axis =
        if (linearLayout is Column) UILayoutConstraintAxis.Vertical
        else UILayoutConstraintAxis.Horizontal
    isLayoutMarginsRelativeArrangement = true
    spacing = linearLayout.spacing.toUiKitSize()
    if (debugModeColors) backgroundColor = UIColor.yellow()
}, treeElement), MosaikViewGroupHolder<UiViewHolder> {

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
        return element is Row && !element.isPacked && weightSum > 0 || super.isFillMaxWidth()
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

class GridViewHolder(
    treeElement: TreeElement
) : UiViewHolder(GridWrapperStack(), treeElement), MosaikViewGroupHolder<UiViewHolder> {

    private val verticalStack = uiView as GridWrapperStack
    private val minColumnWidth = when ((treeElement.element as Grid).elementSize) {
        Grid.ElementSize.MIN -> 180.0
        Grid.ElementSize.SMALL -> 300.0
        Grid.ElementSize.MEDIUM -> 420.0
        Grid.ElementSize.LARGE -> 520.0
    }

    private var numColumns = 1
    private val children = ArrayList<UiViewHolder>()
    private val rowStacks = ArrayList<UIStackView>()

    private fun changedWidth(width: Double) {
        val newColumnCount = max(1, (width / minColumnWidth).toInt())
        if (newColumnCount != numColumns) {
            numColumns = newColumnCount
            relayout()
        }
    }

    private fun relayout() {
        LogUtils.logDebug(this.javaClass.simpleName, "Relayouting $numColumns columns")
        // remove everything
        clearRows()

        // add all children to their row
        children.forEachIndexed { idx, child ->
            val uiViewToAdd = child.uiView
            if (numColumns > 1) {
                val row = idx / numColumns
                while (rowStacks.size <= row)
                    addNewRow()

                val rowStack = rowStacks[row]
                rowStack.addArrangedSubview(uiViewToAdd)
            } else {
                verticalStack.addArrangedSubview(uiViewToAdd)
            }
        }

        // add empty views in last row
        val lastLineElementNum = children.size % numColumns
        if (lastLineElementNum > 0) {
            for (i in 1..numColumns - lastLineElementNum) {
                rowStacks[rowStacks.size - 1].addArrangedSubview(UIView(CGRect.Zero()))
            }
        }
    }

    private fun addNewRow() {
        val newStack = UIStackView().apply {
            axis = UILayoutConstraintAxis.Horizontal
            distribution = UIStackViewDistribution.FillEqually
        }
        verticalStack.addArrangedSubview(newStack)
        rowStacks.add(newStack)
    }

    override fun addSubView(subviewHolder: UiViewHolder) {
        children.add(subviewHolder)
        if (children.size == treeElement.children.size)
            relayout()
    }

    override fun removeAllChildren() {
        clearRows()
        children.clear()
    }

    private fun clearRows() {
        rowStacks.forEach { it.clearArrangedSubviews() }
        verticalStack.clearArrangedSubviews()
        rowStacks.clear()
    }

    override fun replaceSubView(oldView: UiViewHolder, newView: UiViewHolder) {
        val idx = children.indexOf(oldView)
        if (idx >= 0) {
            children[idx] = newView
            relayout()
        }
    }

    override fun isFillMaxWidth(): Boolean = true

    override fun onAddedToSuperview() {
        super.onAddedToSuperview()
        verticalStack.parentHolder = this
    }

    private class GridWrapperStack : UIStackView(CGRect.Zero()) {
        init {
            if (debugModeColors) backgroundColor = UIColor.green()
            layoutMargins = UIEdgeInsets.Zero()
            axis = UILayoutConstraintAxis.Vertical
            isLayoutMarginsRelativeArrangement = true
        }

        var parentHolder: GridViewHolder? = null

        override fun setBounds(v: CGRect) {
            super.setBounds(v)

            parentHolder?.changedWidth(v.size.width)
        }
    }
}

class SeparatorHolder(
    treeElement: TreeElement,
) : UiViewHolder(
    UIView(CGRect.Zero()),
    treeElement
) {

    init {
        val separator = treeElement.element as HorizontalRule

        val padding = separator.getvPadding().toUiKitSize()
        uiView.layoutMargins = UIEdgeInsets(padding, 0.0, padding, 0.0)

        val separatorView = createHorizontalSeparator()
        uiView.addSubview(separatorView)
        separatorView.edgesToSuperview()
    }

    override fun isFillMaxWidth(): Boolean {
        return true
    }
}