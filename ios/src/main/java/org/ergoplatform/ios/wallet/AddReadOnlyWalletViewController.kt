package org.ergoplatform.ios.wallet

import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.AddReadOnlyWalletUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class AddReadOnlyWalletViewController : ViewControllerWithKeyboardLayoutGuide() {
    lateinit var tvAddress: UITextField
    lateinit var errorLabel: Body1Label

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_LABEL_READONLY_WALLET)
        view.backgroundColor = UIColor.systemBackground()
        val uiLogic = IosAddReadOnlyWalletUiLogic(IosStringProvider(texts))

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
        tvAddress = createTextField()
        tvAddress.returnKeyType = UIReturnKeyType.Done
        tvAddress.delegate = object : UITextFieldDelegateAdapter() {
            override fun shouldReturn(textField: UITextField?): Boolean {
                userPressedDone(uiLogic)
                return super.shouldReturn(textField)
            }
        }

        errorLabel = Body1Label()
        errorLabel.textColor = UIColor.red()

        val centerView = UIView(CGRect.Zero())
        centerView.addSubviews(listOf(descLabel, tvAddress, errorLabel))
        container.addSubview(centerView)

        centerView.topToSuperview()
            .widthMatchesSuperview(false, DEFAULT_MARGIN, MAX_WIDTH)
            .bottomToKeyboard(this)

        descLabel.widthMatchesSuperview()
        tvAddress.widthMatchesSuperview().fixedHeight(DEFAULT_TEXT_FIELD_HEIGHT)
            .topToBottomOf(descLabel, DEFAULT_MARGIN * 2).centerVertical()
        errorLabel.widthMatchesSuperview().topToBottomOf(tvAddress, DEFAULT_MARGIN)

    }

    private fun userPressedDone(uiLogic: IosAddReadOnlyWalletUiLogic) {
        val success = uiLogic.addWalletToDb(tvAddress.text, getAppDelegate().database)
        if (success) {
            navigationController.dismissViewController(true) {}
        }
    }

    inner class IosAddReadOnlyWalletUiLogic(stringProvider: IosStringProvider) :
        AddReadOnlyWalletUiLogic(stringProvider) {

        override fun setErrorMessage(message: String) {
            errorLabel.text = message
        }
    }
}