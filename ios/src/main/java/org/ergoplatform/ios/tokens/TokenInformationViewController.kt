package org.ergoplatform.ios.tokens

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic
import org.robovm.apple.uikit.*

class TokenInformationViewController(
    private val tokenId: String, private val tokenAmount: Long?
) : CoroutineViewController() {

    private lateinit var activityView: UIActivityIndicatorView
    private lateinit var closeButton: UIButton
    private lateinit var tokenInfoLayout: TokenInformationLayoutView

    val uiLogic = IosTokenInformationModelLogic()

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()

        activityView = UIActivityIndicatorView(UIActivityIndicatorViewStyle.Large)
        view.addSubview(activityView)
        activityView.centerVertical().centerHorizontal()

        tokenInfoLayout = TokenInformationLayoutView(this)
        val scrollView = tokenInfoLayout.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        closeButton = addCloseButton()
        scrollView.topToBottomOf(closeButton).widthMatchesSuperview().bottomToSuperview()

        tokenInfoLayout.isHidden = true

    }

    override fun viewWillAppear(animated: Boolean) {
        // iOS behaviour: when view is dragged down not completely, it moves up again calling
        // this callback. Check here to avoid unnecessary loading and layouting
        if (!uiLogic.initialized) {
            val appDelegate = getAppDelegate()
            activityView.startAnimating()
            uiLogic.init(tokenId, appDelegate.database, appDelegate.prefs)
        }
    }

    inner class IosTokenInformationModelLogic : TokenInformationModelLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun onTokenInfoUpdated(tokenInformation: TokenInformation?) {
            runOnMainThread {
                activityView.isHidden = true
                tokenInformation?.let {
                    tokenInfoLayout.isHidden = false
                    tokenInfoLayout.updateTokenInformation(tokenAmount)
                } ?: run {
                    val errorView = UIImageView(getIosSystemImage(IMAGE_WARNING, UIImageSymbolScale.Large)).apply {
                        contentMode = UIViewContentMode.ScaleAspectFit
                        tintColor = UIColor.secondaryLabel()
                        fixedHeight(100.0)
                    }
                    view.addSubview(errorView)
                    errorView.centerVertical().centerHorizontal()
                }
            }
        }

        override fun onDownloadStateUpdated() {
            runOnMainThread {
                tokenInfoLayout.updateNftPreview()
            }
        }
    }
}