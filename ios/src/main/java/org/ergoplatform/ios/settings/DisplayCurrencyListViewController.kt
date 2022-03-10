package org.ergoplatform.ios.settings

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_LABEL_CG_CONN_ERROR
import org.ergoplatform.uilogic.STRING_LABEL_NONE
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class DisplayCurrencyListViewController(private val clickListener: (String) -> Unit) : CoroutineViewController() {

    private lateinit var activityView: UIActivityIndicatorView
    private lateinit var closeButton: UIButton
    private lateinit var errorLabel: UILabel

    private var hasListLoaded = false

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackground()

        activityView = UIActivityIndicatorView(UIActivityIndicatorViewStyle.Large)
        view.addSubview(activityView)
        activityView.centerVertical().centerHorizontal()

        closeButton = addCloseButton()

        errorLabel = Body1BoldLabel()
        errorLabel.textAlignment = NSTextAlignment.Center
        errorLabel.text = getAppDelegate().texts.get(STRING_LABEL_CG_CONN_ERROR)
        view.addSubview(errorLabel)
        errorLabel.edgesToSuperview(inset = DEFAULT_MARGIN)

        errorLabel.isHidden = true
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        if (!hasListLoaded) {
            activityView.startAnimating()
            val nodeConnector = WalletStateSyncManager.getInstance()
            nodeConnector.fetchCurrencies()
            viewControllerScope.launch {
                nodeConnector.currencies.collect {
                    runOnMainThread { showCurrencyList(it) }
                }
            }
        }
    }

    private fun showCurrencyList(currencies: List<String>?) {
        // this should only fire once with a list, but let's make sure
        if (!hasListLoaded) {
            activityView.isHidden = currencies != null
            errorLabel.isHidden = !(currencies != null && currencies.isEmpty())

            if (currencies != null && currencies.isNotEmpty()) {
                hasListLoaded = true
                activityView.stopAnimating()

                val listStackView = UIStackView(CGRect.Zero())
                listStackView.axis = UILayoutConstraintAxis.Vertical
                listStackView.spacing = DEFAULT_MARGIN

                listStackView.addArrangedSubview(getCurrencyListEntry(""))
                currencies.sorted().forEach {
                    listStackView.addArrangedSubview(getCurrencyListEntry(it))
                }

                val scrollView = listStackView.wrapInVerticalScrollView()
                view.addSubview(scrollView)
                scrollView.widthMatchesSuperview().bottomToSuperview(bottomInset = DEFAULT_MARGIN)
                    .topToBottomOf(closeButton, inset = DEFAULT_MARGIN)
            }
        }
    }

    private fun getCurrencyListEntry(currency: String): UIView {
        val button =
            TextButton(
                if (currency.isEmpty()) getAppDelegate().texts.get(STRING_LABEL_NONE) else currency.uppercase(),
                fontSize = FONT_SIZE_HEADLINE2
            )
        button.setTitleColor(UIColor.label(), UIControlState.Normal)
        button.addOnTouchUpInsideListener { _, _ ->
            clickListener.invoke(currency)
            dismissViewController(true) {}
        }
        return button
    }
}