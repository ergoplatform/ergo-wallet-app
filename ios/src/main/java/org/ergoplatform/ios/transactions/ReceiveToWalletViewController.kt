package org.ergoplatform.ios.transactions

import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.addresses.ChooseAddressListDialogViewController
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.STRING_LABEL_AMOUNT
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic
import org.ergoplatform.utils.inputTextToDouble
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
    private lateinit var inputErgoAmount: UITextField

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_BUTTON_RECEIVE)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        walletTitle = Headline2Label()
        walletTitle.numberOfLines = 1
        walletTitle.textColor = uiColorErgo
        addressNameLabel = Body1BoldLabel().apply {
            numberOfLines = 1
            textColor = uiColorErgo
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                presentViewController(
                    ChooseAddressListDialogViewController(walletId, false) {
                        uiLogic.derivationIdx = it ?: 0
                        addressChanged()
                    }, true
                ) {}
            })
            // TODO addresses show IMAGE_OPEN_LIST
        }

        val uiBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Action)
        uiBarButtonItem.setOnClickListener {
            uiLogic.getTextToShare(getInputAmount(), getInputPurpose())?.let {
                this@ReceiveToWalletViewController.shareText(
                    it,
                    uiBarButtonItem.keyValueCoder.getValue("view") as UIView
                )
            }
        }
        navigationController.topViewController.navigationItem.rightBarButtonItem = uiBarButtonItem

        qrCode = UIImageView(CGRect.Zero())
        val qrCodeContainer = UIView()
        qrCodeContainer.addSubview(qrCode)
        addressLabel = Headline2Label()
        addressLabel.isUserInteractionEnabled = true
        addressLabel.addGestureRecognizer(UITapGestureRecognizer {
            shareText(addressLabel.text, addressLabel)
        })

        inputErgoAmount = createTextField().apply {
            placeholder = texts.get(STRING_LABEL_AMOUNT)
            keyboardType = UIKeyboardType.NumbersAndPunctuation
            returnKeyType = UIReturnKeyType.Next
            delegate = object : OnlyNumericInputTextFieldDelegate() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    resignFirstResponder()
                    return true
                }
            }
            addOnEditingChangedListener {
                refreshQrCode()
            }
        }

        val container = UIView()
        val stackView = UIStackView(
            NSArray(
                walletTitle,
                addressNameLabel,
                qrCodeContainer,
                addressLabel,
                inputErgoAmount
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, addressNameLabel)
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, qrCodeContainer)
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, addressLabel)
        val scrollView = container.wrapInVerticalScrollView()
        qrCode.fixedWidth(300.0).fixedHeight(300.0).centerHorizontal().topToSuperview().bottomToSuperview()
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
        uiLogic.getTextToShare(getInputAmount(), getInputPurpose())?.let {
            qrCode.setQrCode(it, 300)
        }
    }

    private fun getInputPurpose() = ""
    private fun getInputAmount() = inputTextToDouble(inputErgoAmount.text)
}