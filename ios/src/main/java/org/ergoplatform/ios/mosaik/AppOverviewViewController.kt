package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.MosaikAppOverviewUiLogic
import org.ergoplatform.uilogic.STRING_HINT_APP_URL
import org.ergoplatform.uilogic.STRING_TITLE_MOSAIK
import org.robovm.apple.uikit.*

class AppOverviewViewController : ViewControllerWithKeyboardLayoutGuide() {
    private val uiLogic = IosMosaikAppOverviewUiLogic()

    private lateinit var inputAddress: UITextField

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val texts = getAppDelegate().texts

        title = texts.get(STRING_TITLE_MOSAIK)

        inputAddress = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Continue
            placeholder = texts.get(STRING_HINT_APP_URL)
            textContentType = UITextContentType.URL

            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    navigateToApp()
                    return true
                }
            }

            setCustomActionField(getIosSystemImage(IMAGE_ARROW_RIGHT, UIImageSymbolScale.Small)!!) {
                navigateToApp()
            }
        }

        val container = UIView()
        container.addSubview(inputAddress)

        inputAddress.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview() // TODO

        // TODO favorites, last visited, suggestions

        // TODO disclaimer, opt in

        val scrollView = container.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.topToSuperview()
            .widthMatchesSuperview()
            .bottomToKeyboard(this, DEFAULT_MARGIN)
        scrollView.setDelaysContentTouches(false)
    }

    private fun navigateToApp() {
        navigateToApp(inputAddress.text, null)
    }

    private fun navigateToApp(appUrl: String, appName: String?) {
        navigationController.pushViewController(
            MosaikViewController(appUrl, appName), true
        )
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        uiLogic.init(getAppDelegate().database)
    }

    private inner class IosMosaikAppOverviewUiLogic : MosaikAppOverviewUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewControllerScope
    }
}