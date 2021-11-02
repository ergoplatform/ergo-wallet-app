package org.ergoplatform.ios.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.robovm.apple.uikit.UIViewController

abstract class CoroutineViewController : UIViewController() {
    private var _viewControllerScope: CoroutineScope? = null
    val viewControllerScope: CoroutineScope
        get() {
            if (_viewControllerScope == null) {
                _viewControllerScope = CoroutineScope(Dispatchers.Default)
            }
            return _viewControllerScope!!
        }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        _viewControllerScope?.cancel()
    }
}