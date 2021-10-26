package org.ergoplatform.ios.wallet

import kotlinx.coroutines.*
import org.ergoplatform.android.getPublicErgoAddressFromMnemonic
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.SqlDelightWalletProvider
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.SaveWalletUiLogic
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class SaveWalletViewController(private val mnemonic: SecretString) : UIViewController() {
    private lateinit var progressIndicator: UIActivityIndicatorView
    private lateinit var scrollView: UIScrollView
    private lateinit var addressLabel: UILabel

    private var viewControllerScope: CoroutineScope? = null

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_TITLE_WALLET_DETAILS)
        view.backgroundColor = UIColor.systemBackground()

        navigationController.navigationBar?.tintColor = uiColorErgo

        val uiLogic = SaveWalletUiLogic()

        val container = UIView()
        scrollView = container.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.edgesToSuperview()

        val introLabel = Body1Label()
        introLabel.text = texts.get(STRING_INTRO_SAVE_WALLET)

        addressLabel = Headline2Label()

        val addressInfoLabel = Body1Label()
        addressInfoLabel.text = texts.get(STRING_INTRO_SAVE_WALLET2)

        val buttonSavePassword = TextButton(texts.get(STRING_BUTTON_SAVE_PASSWORD_ENCRYPTED))
        buttonSavePassword.addOnTouchUpInsideListener { _, _ ->
            GlobalScope.launch(Dispatchers.IO) {
                val publicErgoAddress = getPublicErgoAddressFromMnemonic(mnemonic)
                uiLogic.suspendSaveToDb(
                    SqlDelightWalletProvider(getAppDelegate().database), IosStringProvider(texts),
                    publicErgoAddress, 0, null
                )
            }
            navigationController.dismissViewController(true) {}
        }

        val savePwInfoLabel =
            Body1Label().apply { text = texts.get(STRING_DESC_SAVE_PASSWORD_ENCRYPTED) }

        val buttonSaveDevice = TextButton(texts.get(STRING_BUTTON_SAVE_DEVICE_ENCRYPTED))
        buttonSaveDevice.isEnabled = false
        val saveDeviceEncInfo =
            Body1Label().apply { text = texts.get(STRING_DESC_SAVE_DEVICE_ENCRYPTED) }

        val addressInfoStack = UIStackView(
            NSArray(
                introLabel,
                addressLabel,
                addressInfoLabel,
                getHorizontalSeparator(),
                buttonSavePassword,
                savePwInfoLabel,
                getHorizontalSeparator(),
                buttonSaveDevice,
                saveDeviceEncInfo
            )
        )
        addressInfoStack.axis = UILayoutConstraintAxis.Vertical
        addressInfoStack.spacing = DEFAULT_MARGIN * 2
        addressInfoStack.arrangedSubviews.forEach { it.widthMatchesSuperview() }
        container.addSubview(addressInfoStack)
        addressInfoStack.setCustomSpacing(DEFAULT_MARGIN * 4, addressInfoLabel)
        addressInfoStack.topToSuperview(false, DEFAULT_MARGIN * 2)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH).bottomToSuperview()

        container.addSubview(addressInfoStack)

        progressIndicator = UIActivityIndicatorView()
        progressIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.Large
        view.addSubview(progressIndicator)
        progressIndicator.centerVertical().centerHorizontal()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope = CoroutineScope(Dispatchers.Default)
        calculateAndShowPkAddress()
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        viewControllerScope?.cancel()
    }

    private fun calculateAndShowPkAddress() {
        scrollView.isHidden = true
        progressIndicator.startAnimating()

        viewControllerScope?.launch {
            val publicErgoAddressFromMnemonic = getPublicErgoAddressFromMnemonic(mnemonic)
            runOnMainThread {
                addressLabel.text = publicErgoAddressFromMnemonic
                progressIndicator.isHidden = true
                scrollView.isHidden = false
            }
        }
    }
}