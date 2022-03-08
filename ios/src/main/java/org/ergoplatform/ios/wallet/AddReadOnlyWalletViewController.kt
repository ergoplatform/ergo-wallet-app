package org.ergoplatform.ios.wallet

import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_INTRO_ADD_READONLY
import org.ergoplatform.uilogic.STRING_LABEL_READONLY_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_READONLY_WALLET_DEFAULT
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_NAME
import org.ergoplatform.uilogic.wallet.AddReadOnlyWalletUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class AddReadOnlyWalletViewController : ViewControllerWithKeyboardLayoutGuide() {
    lateinit var tvAddress: UITextField
    lateinit var errorLabel: Body1Label
    lateinit var tvDisplayName: UITextField

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_LABEL_READONLY_WALLET)
        view.backgroundColor = UIColor.systemBackground()
        val uiLogic = IosAddReadOnlyWalletUiLogic()

        if (navigationController.viewControllers.size == 1) {
            val cancelButton = UIBarButtonItem(UIBarButtonSystemItem.Cancel)
            cancelButton.setOnClickListener { this.dismissViewController(true) {} }
            navigationItem.leftBarButtonItem = cancelButton
        }

        navigationController.navigationBar?.tintColor = uiColorErgo

        val nextButton = UIBarButtonItem(UIBarButtonSystemItem.Done)
        navigationItem.rightBarButtonItem = nextButton
        nextButton.setOnClickListener {
            userPressedDone(uiLogic)
        }

        val descLabel = Body1Label()
        descLabel.text = texts.get(STRING_INTRO_ADD_READONLY)
        tvAddress = createTextField().apply {
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    tvDisplayName.becomeFirstResponder()
                    return super.shouldReturn(textField)
                }
            }

            setCustomActionField(getIosSystemImage(IMAGE_QR_SCAN, UIImageSymbolScale.Small)!!) {
                presentViewController(QrScannerViewController(invokeAfterDismissal = false) {
                    text = uiLogic.getInputFromQrCode(it)
                }, true) {}
            }
        }


        errorLabel = Body1Label()
        errorLabel.textColor = UIColor.systemRed()

        val labelDisplayName = Body1BoldLabel().apply {
            textColor = uiColorErgo
            text = texts.get(STRING_LABEL_WALLET_NAME)
        }

        tvDisplayName = createTextField().apply {
            clearButtonMode = UITextFieldViewMode.Always
            text = texts.get(STRING_LABEL_READONLY_WALLET_DEFAULT)
            returnKeyType = UIReturnKeyType.Done
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    userPressedDone(uiLogic)
                    return true
                }
            }
        }

        val centerView = UIView(CGRect.Zero())
        centerView.addSubviews(
            listOf(
                descLabel,
                tvAddress,
                errorLabel,
                labelDisplayName,
                tvDisplayName
            )
        )
        container.addSubview(centerView)

        centerView.topToSuperview()
            .widthMatchesSuperview(false, DEFAULT_MARGIN, MAX_WIDTH)
            .bottomToKeyboard(this)

        descLabel.widthMatchesSuperview()
        tvAddress.widthMatchesSuperview()
            .topToBottomOf(descLabel, DEFAULT_MARGIN * 2).centerVertical()
        errorLabel.widthMatchesSuperview().topToBottomOf(tvAddress, DEFAULT_MARGIN)
        labelDisplayName.widthMatchesSuperview().topToBottomOf(errorLabel, DEFAULT_MARGIN * 3)
        tvDisplayName.widthMatchesSuperview().topToBottomOf(labelDisplayName, DEFAULT_MARGIN)

    }

    private fun userPressedDone(uiLogic: IosAddReadOnlyWalletUiLogic) {
        viewControllerScope.launch {
            val appDelegate = getAppDelegate()
            val success = uiLogic.addWalletToDb(tvAddress.text, appDelegate.database.walletDbProvider,
                IosStringProvider(appDelegate.texts), tvDisplayName.text)
            if (success) {
                runOnMainThread {
                    navigationController.dismissViewController(true) {}
                }
            }
        }
    }

    inner class IosAddReadOnlyWalletUiLogic : AddReadOnlyWalletUiLogic() {

        override fun setErrorMessage(message: String) {
            runOnMainThread {
                errorLabel.text = message
                tvAddress.setHasError(message.isNotEmpty())
            }
        }
    }
}