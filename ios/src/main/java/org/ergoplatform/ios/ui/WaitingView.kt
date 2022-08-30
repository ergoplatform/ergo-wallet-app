package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIActivityIndicatorView
import org.robovm.apple.uikit.UIActivityIndicatorViewStyle
import org.robovm.apple.uikit.UIView

/**
 * Label with a loading indicator attached on its left side
 */
class WaitingView : UIView(CGRect.Zero()) {
    private val loadingIndicator = UIActivityIndicatorView().apply {
        activityIndicatorViewStyle = UIActivityIndicatorViewStyle.Medium
    }

    private val label = Body1Label()

    init {
        addSubview(loadingIndicator)
        addSubview(label)

        loadingIndicator.leftToSuperview().superViewWrapsHeight().centerVertical()
        label.superViewWrapsHeight().rightToSuperview().centerVertical()
            .leftToRightOf(loadingIndicator, inset = DEFAULT_MARGIN)
    }

    fun showWaitingText(text: String) {
        loadingIndicator.startAnimating()
        label.text = text
    }
}
