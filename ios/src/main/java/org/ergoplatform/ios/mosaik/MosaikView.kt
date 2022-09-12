package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.ui.layout.Column
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSLayoutConstraint
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UIView
import java.util.*

class MosaikView(
    private val mosaikRuntime: AppMosaikRuntime
) : UIView(CGRect.Zero()) {

    private var root: ViewHolderWithChildren? = null

    init {
        layoutMargins = UIEdgeInsets.Zero()
    }

    fun updateView() {
        val rootElement = mosaikRuntime.viewTree.content
        val setRootView: (UiViewHolder) -> Unit = { viewHolder ->
            val uiView = viewHolder.uiView
            addSubview(uiView)

            val positionAtTop = (viewHolder.treeElement.element is Column)

            val maxWidth = when (viewHolder.treeElement.viewTree.targetCanvasDimension) {
                MosaikManifest.CanvasDimension.COMPACT_WIDTH -> 500.0
                MosaikManifest.CanvasDimension.MEDIUM_WIDTH -> 720.0
                else -> 0.0
            }
            uiView.centerHorizontal(true)
                .topToSuperview(canBeMore = !positionAtTop)
                .bottomToSuperview(canBeLess = true)

            if (maxWidth > 0)
                NSLayoutConstraint.activateConstraints(
                    NSArray(
                        uiView.widthAnchor.lessThanOrEqualTo(maxWidth)
                    )
                )

            if (!positionAtTop)
                uiView.centerVertical()
        }
        if (rootElement == null) {
            root?.removeAllChildren()
            root = null
        } else if (root == null) {
            root = ViewHolderWithChildren(rootElement)
            setRootView(root!!.uiViewHolder)
            root?.updateChildren(true)
        } else {
            root?.updateView(rootElement, replaceOnParent = { _, newViewHolder ->
                subviews.forEach { it.removeFromSuperview() }
                setRootView(newViewHolder)
            })
        }

    }

}

/**
 * This class holds the UiViewHolder and its children and manages the updates to the view tree
 */
class ViewHolderWithChildren(
    treeElement: TreeElement
) {

    var treeElement = treeElement
        private set
    var uiViewHolder: UiViewHolder = MosaikViewCommon.buildUiViewHolder(treeElement)
        private set
    private val children: MutableList<ViewHolderWithChildren> = LinkedList<ViewHolderWithChildren>()

    fun updateView(
        newTreeElement: TreeElement,
        replaceOnParent: (oldView: UiViewHolder, newView: UiViewHolder) -> Unit,
    ) {
        val elementChanged = if (newTreeElement.createdAtContentVersion > treeElement.createdAtContentVersion) {
            treeElement = newTreeElement

            // if the element changed, remove it from parent and readd the new one
            val newViewHolder = MosaikViewCommon.buildUiViewHolder(treeElement)
            replaceOnParent(uiViewHolder, newViewHolder)
            newViewHolder.onAddedToSuperview()

            uiViewHolder = newViewHolder
            true
        } else false

        // resource bytes might have an update
        treeElement.getResourceBytes?.let {
            uiViewHolder.resourceBytesAvailable(it)
        }

        updateChildren(elementChanged)
    }

    fun updateChildren(parentChanged: Boolean) {
        val viewGroupHolder = uiViewHolder as? ViewGroupHolder ?: return
        val newChildren = treeElement.children

        // check if a children are to be added or removed
        // this is the case when the parent was changed or children aren't initialized yet
        val childAddedOrRemoved = parentChanged || newChildren.size != children.size

        if (childAddedOrRemoved) {
            // remove all children
            removeAllChildren()

            // then add the new elements
            newChildren.forEach {
                val newElem = ViewHolderWithChildren(it)
                viewGroupHolder.addSubView(newElem.uiViewHolder)
                newElem.uiViewHolder.onAddedToSuperview()
                children.add(newElem)
            }
        }

        children.zip(newChildren).forEach { (childViewHolder, newChild) ->
            childViewHolder.updateView(
                newChild, // might be the old child, updateview will check
                viewGroupHolder::replaceSubView
            )
        }
    }

    fun removeAllChildren() {
        (uiViewHolder as? ViewGroupHolder)?.removeAllChildren()
        children.clear()
    }


}