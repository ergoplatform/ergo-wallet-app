package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.bottomToSuperview
import org.ergoplatform.ios.ui.centerHorizontal
import org.ergoplatform.ios.ui.centerVertical
import org.ergoplatform.ios.ui.topToSuperview
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.MosaikViewHolderManager
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.ui.layout.Column
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSLayoutConstraint
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UIView

class MosaikView(
    private val mosaikRuntime: AppMosaikRuntime
) : UIView(CGRect.Zero()) {

    private var rootViewHolderManager: MosaikViewHolderManager<UiViewHolder>? = null

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
            rootViewHolderManager?.removeAllChildren()
            rootViewHolderManager = null
        } else if (rootViewHolderManager == null) {
            rootViewHolderManager = MosaikViewHolderManager(rootElement, MosaikViewCommon::buildUiViewHolder)
            setRootView(rootViewHolderManager!!.mosaikViewHolder)
            rootViewHolderManager?.updateChildren(true)
        } else {
            rootViewHolderManager?.updateView(rootElement, replaceOnParent = { _, newViewHolder ->
                subviews.forEach { it.removeFromSuperview() }
                setRootView(newViewHolder)
            })
        }

    }

}