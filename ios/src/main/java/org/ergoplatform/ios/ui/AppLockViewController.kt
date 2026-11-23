package org.ergoplatform.ios.ui

import org.ergoplatform.ios.api.IosAuthentication
import org.ergoplatform.uilogic.STRING_BUTTON_UNLOCK
import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.uikit.*

class AppLockViewController : UIViewController() {

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackground()

        val stackView = UIStackView().apply {
            alignment = UIStackViewAlignment.Center
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN * 5
        }

        val ergoLogo = UIImageView(ergoLogoImage.imageWithTintColor(uiColorErgo)).apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(200.0)
        }

        val loginButton = CommonButton(getAppDelegate().texts.get(STRING_BUTTON_UNLOCK))

        loginButton.addOnTouchUpInsideListener { _, _ -> doAuth() }

        stackView.addArrangedSubview(ergoLogo)
        stackView.addArrangedSubview(loginButton)

        view.addSubview(stackView)
        stackView.centerVertical().widthMatchesSuperview()
    }

    private fun doAuth() {
        IosAuthentication.authenticate(getAppDelegate().texts.get(STRING_BUTTON_UNLOCK),
            object : IosAuthentication.IosAuthCallback {
                override fun onAuthenticationSucceeded(context: LAContext) {
                    getAppDelegate().appUnlocked()
                    runOnMainThread { dismissViewController(true) {} }
                }

                override fun onAuthenticationError(error: String) {

                }

                override fun onAuthenticationCancelled() {

                }

            }
        )
    }

    fun presentModalAbove(vc: UIViewController) {
        if (vc is AppLockViewController)
            return

        modalPresentationStyle = UIModalPresentationStyle.OverCurrentContext
        isModalInPresentation = true
        vc.presentViewController(this, false) {}
    }
}
