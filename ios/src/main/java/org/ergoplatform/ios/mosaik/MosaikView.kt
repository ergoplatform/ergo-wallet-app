package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.centerHorizontal
import org.ergoplatform.ios.ui.clearArrangedSubviews
import org.ergoplatform.ios.ui.superViewWrapsHeight
import org.ergoplatform.ios.ui.topToSuperview
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.TreeElement
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UIView
import java.util.*

class MosaikView(
    private val mosaikRuntime: AppMosaikRuntime
) : UIView(CGRect.Zero()) {

    private var root: ViewWithTreeElement? = null

    init {
        layoutMargins = UIEdgeInsets.Zero()
    }

    fun updateView() {
        val rootElement = mosaikRuntime.viewTree.content

        if (rootElement == null) {
            root?.removeAllChildren()
            root = null
        } else if (root == null) {
            root = ViewWithTreeElement(rootElement)
            val uiView = root!!.uiView
            addSubview(uiView)
            uiView.centerHorizontal(true).topToSuperview().superViewWrapsHeight()
            root?.updateChildren()
        } else {
            root?.updateView(rootElement)
        }

    }

}

class ViewWithTreeElement(
    treeElement: TreeElement
) {

    var treeElement = treeElement
        private set
    var uiView: UIView = MosaikUiViewFactory.createUiViewForTreeElement(treeElement)
        private set
    val children: MutableList<ViewWithTreeElement> = LinkedList<ViewWithTreeElement>()

    fun addSubview(element: ViewWithTreeElement) {
        val viewToAdd = element.uiView

        when (val thisView = this.uiView) {
            is UIStackView -> {
                // TODO different layouts, this is always JUSTIFY without weight
                thisView.addArrangedSubview(viewToAdd)
            }
            else -> {
                thisView.addSubview(viewToAdd)
                viewToAdd.centerHorizontal(true).topToSuperview().superViewWrapsHeight()
            }
        }
        children.add(element)
    }

    fun updateView(
        newTreeElement: TreeElement,
    ) {
        if (newTreeElement.createdAtContentVersion > treeElement.createdAtContentVersion) {
            treeElement = newTreeElement

            // TODO if the element changed, remove it from parent and readd the new one
        }

        // resource bytes might have an update
        treeElement.getResourceBytes?.let {
            MosaikUiViewFactory.resourceBytesAvailable(uiView, it)
        }

        updateChildren()
    }

    fun updateChildren() {
        if (treeElement.children.map { it.createdAtContentVersion } !=
            children.map { it.treeElement.createdAtContentVersion }) {
            // something changed

            // remove all children
            removeAllChildren()

            // then add the new elements
            treeElement.children.forEach {
                val newElem = ViewWithTreeElement(it)
                addSubview(newElem)
            }

            // TODO room for improvement: swap a single element when only one has changed
        }

        children.forEach {
            it.updateView(it.treeElement)
        }
    }

    fun removeAllChildren() {
        when (val uiView = uiView) {
            is UIStackView -> uiView.clearArrangedSubviews()
            else -> uiView.subviews.forEach { it.removeFromSuperview() }
        }
        children.clear()
    }


}