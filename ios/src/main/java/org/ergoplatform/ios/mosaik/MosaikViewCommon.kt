package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.MosaikViewHolder
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.*
import org.ergoplatform.mosaik.model.ui.input.*
import org.ergoplatform.mosaik.model.ui.layout.*
import org.ergoplatform.mosaik.model.ui.text.Button
import org.ergoplatform.mosaik.model.ui.text.StyleableTextLabel
import org.ergoplatform.mosaik.model.ui.text.TokenLabel
import org.robovm.apple.uikit.*

const val debugModeColors = false

object MosaikViewCommon {
    fun buildUiViewHolder(treeElement: TreeElement): UiViewHolder {
        val mosaikViewElement = treeElement.element

        val uiViewHolder = when (mosaikViewElement) {

            is Box -> BoxViewHolder(treeElement)

            is CheckboxLabel -> CheckboxViewHolder(treeElement)

            is StyleableTextLabel<*> -> LabelViewHolder(treeElement, mosaikViewElement)

            is TokenLabel -> TokenLabelHolder(treeElement)

            is LinearLayout<*> -> StackViewHolder(mosaikViewElement, treeElement)

            is Button -> ButtonHolder(treeElement)

            is ErgAmountInputField -> ErgAmountInputHolder(treeElement)

            is TextField<*> -> TextFieldViewHolder(treeElement)

            is DropDownList -> DropDownViewHolder(treeElement)

            is LoadingIndicator -> LoadingIndicatorHolder(treeElement)

            is Icon -> IconImageViewHolder(treeElement)

            is Image -> ImageViewHolder(treeElement)

            is QrCode -> QrCodeViewHolder(treeElement)

            is StyleableInputButton<*> -> InputButtonHolder(treeElement)

            is HorizontalRule -> SeparatorHolder(treeElement)

            is MarkDown -> MarkDownHolder(treeElement)

            is Grid -> GridViewHolder(treeElement)

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

    /**
     * returns true if the element is not only to be maximized within its parent, but should
     * make its parent maximize in its parent and so on
     */
    fun shouldExpandElement(element: ViewElement): Boolean =
        element is Grid
                || element is ViewGroup && element.children.find { shouldExpandElement(it) } != null
}

open class UiViewHolder(val uiView: UIView, val treeElement: TreeElement) : MosaikViewHolder {
    override fun resourceBytesAvailable(bytes: ByteArray) {
    }

    override fun onAddedToSuperview() {
        if (isFillMaxWidth()) {
            // lower priority to not overrule max width constraints
            uiView.widthMatchesSuperview(priority = iosDefaultPriority - 1)
        }
    }

    override fun onRemovedFromSuperview() {
    }

    open fun isFillMaxWidth(): Boolean = MosaikViewCommon.shouldExpandElement(treeElement.element)

    open val handlesClicks: Boolean = false
}

fun Padding.toUiKitSize() =
    when (this) {
        Padding.NONE -> 0.0
        Padding.QUARTER_DEFAULT -> DEFAULT_MARGIN * .5
        Padding.HALF_DEFAULT -> DEFAULT_MARGIN * 1
        Padding.DEFAULT -> DEFAULT_MARGIN * 2
        Padding.ONE_AND_A_HALF_DEFAULT -> DEFAULT_MARGIN * 3
        Padding.TWICE -> DEFAULT_MARGIN * 4
    }