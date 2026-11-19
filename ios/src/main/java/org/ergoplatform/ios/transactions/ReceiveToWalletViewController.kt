package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.launch
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.STRING_HINT_AMOUNT_CURRENCY
import org.ergoplatform.uilogic.STRING_LABEL_AMOUNT
import org.ergoplatform.uilogic.wallet.ReceiveToWalletUiLogic
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ReceiveToWalletViewController(private val walletId: Int, derivationIdx: Int = 0) :
    ViewControllerWithKeyboardLayoutGuide() {
    val uiLogic = ReceiveToWalletUiLogic().apply { this.derivationIdx = derivationIdx }

    private lateinit var walletTitle: UILabel
    private lateinit var addressLabel: UILabel
    private lateinit var qrCode: UIImageView
    private lateinit var addressNameLabel: UILabel
    private lateinit var inputAmount: UITextField
    private lateinit var otherCurrencyAmount: Body1Label
    private lateinit var otherCurrencyContainer: UIView

    private lateinit var texts: I18NBundle

    override fun viewDidLoad() {
        super.viewDidLoad()

        texts = getAppDelegate().texts
        title = texts.get(STRING_BUTTON_RECEIVE)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        walletTitle = Headline2Label()
        walletTitle.numberOfLines = 1
        walletTitle.textColor = uiColorErgo
        val addressNameContainer = buildAddressSelectorView(this, walletId, false) {
            uiLogic.derivationIdx = it ?: 0
            addressChanged()
        }
        addressNameLabel = addressNameContainer.content

        val uiBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Action)
        uiBarButtonItem.setOnClickListener {
            uiLogic.getTextToShare(getInputPurpose())?.let {
                this@ReceiveToWalletViewController.shareText(
                    it,
                    uiBarButtonItem
                )
            }
        }
        navigationController.topViewController.navigationItem.rightBarButtonItem = uiBarButtonItem

        qrCode = UIImageView(CGRect.Zero()).apply {
            contentMode = UIViewContentMode.ScaleAspectFit
        }
        val qrCodeContainer = UIView()
        qrCodeContainer.addSubview(qrCode)
        addressLabel = Headline2Label().apply {
            textAlignment = NSTextAlignment.Center
        }
        val addressLabelWithImage =
            addressLabel.wrapWithTrailingImage(
                getIosSystemImage(IMAGE_SHARE, UIImageSymbolScale.Small)!!,
                keepWidth = true, inset = DEFAULT_MARGIN * 1.5
            ).apply {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    HapticFeedback().perform(UIImpactFeedbackStyle.Medium)
                    shareText(addressLabel.text, addressLabel)
                })
            }

        inputAmount = EndIconTextField().apply {
            keyboardType = UIKeyboardType.NumbersAndPunctuation
            returnKeyType = UIReturnKeyType.Next
            delegate = object : OnlyNumericInputTextFieldDelegate() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    resignFirstResponder()
                    return true
                }
            }
            addOnEditingChangedListener {
                uiLogic.amountToReceive.inputAmountChanged(text)
                refreshQrCode()
                setInputAmountLabels()
            }
        }
        otherCurrencyAmount = Body1Label().apply {
            textColor = UIColor.secondaryLabel()
            textAlignment = NSTextAlignment.Right
        }
        otherCurrencyContainer = otherCurrencyAmount
            .wrapWithTrailingImage(
                getIosSystemImage(IMAGE_EDIT_CIRCLE, UIImageSymbolScale.Small, 20.0)!!,
                keepWidth = true
            ).apply {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    val changed = uiLogic.amountToReceive.switchInputAmountMode()
                    if (changed) {
                        getAppDelegate().prefs.isSendInputFiatAmount = uiLogic.amountToReceive.inputIsFiat
                        setInputAmountLabels()
                        inputAmount.text = uiLogic.amountToReceive.getInputAmountString()
                    }
                })
            }

        val container = UIView()
        val stackView = UIStackView(
            NSArray(
                walletTitle,
                addressNameContainer,
                qrCodeContainer,
                addressLabelWithImage,
                inputAmount,
                otherCurrencyContainer
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, addressNameContainer)
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, qrCodeContainer)
        stackView.setCustomSpacing(DEFAULT_MARGIN * 2, addressLabelWithImage)
        val scrollView = container.wrapInVerticalScrollView()
        qrCode.fixedWidth(DEFAULT_QR_CODE_SIZE).fixedHeight(DEFAULT_QR_CODE_SIZE).centerHorizontal().topToSuperview()
            .bottomToSuperview()
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.topToSuperview().widthMatchesSuperview().bottomToKeyboard(this)

        if (getAppDelegate().prefs.isSendInputFiatAmount != uiLogic.amountToReceive.inputIsFiat) {
            uiLogic.amountToReceive.switchInputAmountMode()
        }
        setInputAmountLabels()
    }

    private fun setInputAmountLabels() {
        inputAmount.placeholder =
            if (uiLogic.amountToReceive.inputIsFiat)
                texts.format(
                    STRING_HINT_AMOUNT_CURRENCY,
                    WalletStateSyncManager.getInstance().fiatCurrency.uppercase()
                )
            else texts.get(STRING_LABEL_AMOUNT)
        val otherCurrencyText = uiLogic.getOtherCurrencyLabel(IosStringProvider(texts))
        otherCurrencyContainer.isHidden = otherCurrencyText == null
        otherCurrencyAmount.text = otherCurrencyText
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            uiLogic.loadWallet(walletId, getAppDelegate().database.walletDbProvider)
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
        uiLogic.getTextToShare(getInputPurpose())?.let {
            qrCode.setQrCode(it, DEFAULT_QR_CODE_SIZE)
        }
    }

    private fun getInputPurpose() = ""
}