package org.ergoplatform.ios.ui

import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSLayoutConstraint
import org.robovm.apple.uikit.UILayoutGuide
import org.robovm.apple.uikit.UIView
import org.robovm.apple.uikit.UIViewController

open class ViewControllerWithKeyboardLayoutGuide : UIViewController() {
    lateinit var keyboardLayoutGuide: UILayoutGuide
    lateinit var container: UIView
    private lateinit var keyboardHeightConstraint: NSLayoutConstraint

    override fun viewDidLoad() {
        super.viewDidLoad()
        keyboardLayoutGuide = UILayoutGuide()
        keyboardHeightConstraint = keyboardLayoutGuide.heightAnchor.equalTo(0.0)

        container = UIView()
        view.addSubview(container)
        container.edgesToSuperview()
        container.addLayoutGuide(keyboardLayoutGuide)
        NSLayoutConstraint.activateConstraints(
            NSArray(
                keyboardHeightConstraint,
                keyboardLayoutGuide.bottomAnchor.equalTo(container.bottomAnchor)
            )
        )
    }

    fun adjustKeyboardHeight(keyboard: CGRect?, duration: Double) {
        val containerViewFrame = view.convertRectToCoordinateSpace(container.frame, view.window)
        keyboardHeightConstraint.constant =
            if (keyboard == null || keyboard.y > containerViewFrame.maxY) 0.0
            else containerViewFrame.maxY - keyboard.y

        UIView.animate(duration) {
            container.layoutIfNeeded()
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        getAppDelegate().addKeyboardObserver(this)
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        getAppDelegate().removeKeyboardObserver(this)
    }
}