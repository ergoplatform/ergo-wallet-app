package org.ergoplatform.ios.wallet

import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.STRING_LABEL_SHARE
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ReceiveToWalletViewController(val walletId: Int, derivationIdx: Int = 0) :
    ViewControllerWithKeyboardLayoutGuide() {
    val uiLogic = ReceiveToWalletUiLogic().apply { this.derivationIdx = derivationIdx }

    private lateinit var walletTitle: UILabel
    private lateinit var addressLabel: UILabel
    private lateinit var qrCode: UIImageView
    private lateinit var addressNameLabel: UILabel

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_BUTTON_RECEIVE)
        view.backgroundColor = UIColor.systemBackground()
        // TODO tinting ui bar items

        walletTitle = Headline2Label()
        walletTitle.numberOfLines = 1
        walletTitle.textColor = uiColorErgo
        addressNameLabel = Body1BoldLabel()
        addressNameLabel.numberOfLines = 1
        addressNameLabel.textColor = uiColorErgo

        val uiBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Action)
        uiBarButtonItem.title = texts.get(STRING_LABEL_SHARE)
        uiBarButtonItem.setOnClickListener {
            // TODO add correct values
            // TODO does not work on ipad
            uiLogic.getTextToShare(0.0, "")?.let { this@ReceiveToWalletViewController.shareText(it) }
        }
        navigationController.topViewController.navigationItem.rightBarButtonItem = uiBarButtonItem

        // TODO copy button

        qrCode = UIImageView(CGRect.Zero())
        addressLabel = Headline2Label()

        val container = UIView()
        val stackView = UIStackView(NSArray(walletTitle, addressNameLabel, qrCode, addressLabel))
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, addressNameLabel)
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, qrCode)
        val scrollView = container.wrapInVerticalScrollView()
        qrCode.fixedWidth(300.0).fixedHeight(300.0)
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.topToSuperview().widthMatchesSuperview().bottomToKeyboard(this)
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            uiLogic.loadWallet(walletId, getAppDelegate().database)
            uiLogic.wallet?.let {
                runOnMainThread {
                    walletTitle.text = it.walletConfig.displayName
                    addressChanged()
                }
            }
        }
    }

    private fun addressChanged() {
        uiLogic.address?.let { address ->
            addressLabel.text = address.publicAddress
            addressNameLabel.text = address.getAddressLabel(IosStringProvider(getAppDelegate().texts))
        }

        refreshQrCode()
    }

    private fun refreshQrCode() {
        // TODO
    }
}