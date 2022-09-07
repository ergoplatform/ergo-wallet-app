package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.centerHorizontal
import org.ergoplatform.ios.ui.superViewWrapsHeight
import org.ergoplatform.ios.ui.topToSuperview
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.TreeElement
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIEdgeInsets
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
            val uiView = root!!.uiViewHolder.uiView
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
    var uiViewHolder: UiViewHolder = MosaikUiViewFactory.createUiViewForTreeElement(treeElement)
        private set
    val children: MutableList<ViewWithTreeElement> = LinkedList<ViewWithTreeElement>()

    fun addSubview(element: ViewWithTreeElement) {
        this.uiViewHolder.addSubView(element.uiViewHolder)
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
            uiViewHolder.resourceBytesAvailable(it)
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
        uiViewHolder.removeAllChildren()
        children.clear()
    }


}