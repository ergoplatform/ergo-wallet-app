package org.ergoplatform.ios.ui

import org.robovm.apple.uikit.*

class ProgressViewController : UIViewController() {
    private lateinit var progressIndicator: UIActivityIndicatorView
    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.black().addAlpha(0.5)

        progressIndicator = UIActivityIndicatorView()
        progressIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.Large

        view.addSubview(progressIndicator)
        progressIndicator.centerVertical().centerHorizontal()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        progressIndicator.startAnimating()
    }

    fun presentModalAbove(vc: UIViewController) {
        modalPresentationStyle = UIModalPresentationStyle.OverCurrentContext
        isModalInPresentation = true
        vc.presentViewController(this, false) {}
    }
}
