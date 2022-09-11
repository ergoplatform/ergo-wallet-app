package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.Icon
import org.ergoplatform.mosaik.model.ui.Image
import org.ergoplatform.mosaik.model.ui.LoadingIndicator
import org.ergoplatform.mosaik.model.ui.QrCode
import org.ergoplatform.mosaik.model.ui.input.CheckboxLabel
import org.ergoplatform.mosaik.model.ui.input.DropDownList
import org.ergoplatform.mosaik.model.ui.input.TextField
import org.ergoplatform.mosaik.model.ui.layout.*
import org.ergoplatform.mosaik.model.ui.text.Button
import org.ergoplatform.mosaik.model.ui.text.StyleableTextLabel
import org.robovm.apple.uikit.*

const val debugModeColors = false

object MosaikViewCommon {
    fun buildUiViewHolder(treeElement: TreeElement): UiViewHolder {
        val mosaikViewElement = treeElement.element
        val runtime = treeElement.viewTree.mosaikRuntime

        val uiViewHolder = when (mosaikViewElement) {

            is Box -> BoxViewHolder(treeElement)

            is CheckboxLabel -> CheckboxViewHolder(treeElement)

            is StyleableTextLabel<*> -> LabelViewHolder(treeElement, mosaikViewElement)

            // TODO is TokenLabel -> MosaikTokenLabel(treeElement, newModifier)

            is LinearLayout<*> -> StackViewHolder(mosaikViewElement, treeElement)

            is Button -> ButtonHolder(treeElement)

            // TODO is ErgAmountInputField -> MosaikErgAmountInputLayout(treeElement, newModifier)

            is TextField<*> -> TextFieldViewHolder(treeElement)

            is DropDownList -> DropDownViewHolder(treeElement)

            is LoadingIndicator -> LoadingIndicatorHolder(treeElement)

            is Icon -> IconImageViewHolder(treeElement)

            is Image -> ImageViewHolder(treeElement)

            is QrCode -> QrCodeViewHolder(treeElement)

            // TODO is StyleableInputButton<*> -> MosaikInputButton(treeElement, newModifier)

            is HorizontalRule -> SeparatorHolder(treeElement)

            // TODO is MarkDown -> MosaikMarkDown(treeElement, newModifier)

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
                if (mosaikViewElement.children.isEmpty())
                    // set a low priority on the fixed height so that justified views get enlarged
                    uiView.fixedHeight(padding * 2, iosDefaultPriority - 500)
                        .fixedWidth(padding * 2, iosDefaultPriority - 500)
                else
                    uiView.minHeight(padding * 2).minWidth(padding * 2)
            }
        }

        if (!uiViewHolder.handlesClicks && treeElement.respondsToClick) {
            uiView.isUserInteractionEnabled = true
            uiView.addGestureRecognizer(UILongPressGestureRecognizer {
                treeElement.longPressed()
            })
            uiView.addGestureRecognizer(UITapGestureRecognizer {
                treeElement.clicked()
            })
        }

        uiView.isHidden = !mosaikViewElement.isVisible

        if (debugModeColors && uiView.backgroundColor == null)
            uiView.backgroundColor = UIColor.blue()

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

    open val handlesClicks: Boolean = false
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