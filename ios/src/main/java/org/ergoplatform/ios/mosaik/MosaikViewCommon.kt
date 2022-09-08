package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.Image
import org.ergoplatform.mosaik.model.ui.layout.Box
import org.ergoplatform.mosaik.model.ui.layout.LayoutElement
import org.ergoplatform.mosaik.model.ui.layout.LinearLayout
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.model.ui.text.StyleableTextLabel
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UILongPressGestureRecognizer
import org.robovm.apple.uikit.UITapGestureRecognizer
import org.robovm.apple.uikit.UIView

const val debugModeColors = false

object MosaikViewCommon {
    fun buildUiViewHolder(treeElement: TreeElement): UiViewHolder {
        val mosaikViewElement = treeElement.element
        val runtime = treeElement.viewTree.mosaikRuntime

        val uiViewHolder = when (mosaikViewElement) {
            is LinearLayout<*> -> StackViewHolder(mosaikViewElement, treeElement)

            is Box -> BoxViewHolder(treeElement)

            is Image -> ImageViewHolder(treeElement)

            is StyleableTextLabel<*> -> LabelViewHolder(treeElement, mosaikViewElement)

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

}

open class UiViewHolder(val uiView: UIView, val treeElement: TreeElement) {
    open fun resourceBytesAvailable(bytes: ByteArray) {
    }

    open fun onAddedToSuperview() {
        if (isFillMaxWidth()) {
            uiView.widthMatchesSuperview()
        }
    }

    open fun isFillMaxWidth(): Boolean = false
}

fun Padding.toUiKitSize() =
    when (this) {
        Padding.NONE -> 0.0
        Padding.QUARTER_DEFAULT -> DEFAULT_MARGIN * .75
        Padding.HALF_DEFAULT -> DEFAULT_MARGIN * 1.5
        Padding.DEFAULT -> DEFAULT_MARGIN * 3
        Padding.ONE_AND_A_HALF_DEFAULT -> DEFAULT_MARGIN * 4.5
        Padding.TWICE -> DEFAULT_MARGIN * 6
    }