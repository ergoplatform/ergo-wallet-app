package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.Body1Label
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.layout.Column
import org.ergoplatform.mosaik.model.ui.layout.Row
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UIView

object MosaikUiViewFactory {
    fun createUiViewForTreeElement(treeElement: TreeElement): UIView {
        val mosaikViewElement = treeElement.element
        val runtime = treeElement.viewTree.mosaikRuntime

        val uiView = when (mosaikViewElement) {
            is Column -> {
                UIStackView().apply {
                    axis = UILayoutConstraintAxis.Vertical
                }
            }
            is Row -> {
                UIStackView().apply {
                    axis = UILayoutConstraintAxis.Horizontal
                }
            }
            else -> {
                Body1Label().apply {
                    text = "Unsupported element: ${mosaikViewElement.javaClass.simpleName}"
                }
            }
        }

        return uiView
    }

}