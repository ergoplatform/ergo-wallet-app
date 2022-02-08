package org.ergoplatform.ios.wallet

import org.ergoplatform.appkit.Mnemonic
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class CreateWalletViewController : UIViewController() {
    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_LABEL_CREATE_WALLET)
        view.backgroundColor = UIColor.systemBackground()

        if (navigationController.viewControllers.size == 1) {
            val cancelButton = UIBarButtonItem(UIBarButtonSystemItem.Cancel)
            cancelButton.setOnClickListener { this.dismissViewController(true) {} }
            navigationItem.leftBarButtonItem = cancelButton
        }

        navigationController.navigationBar?.tintColor = uiColorErgo

        val nextButton = UIBarButtonItem(UIBarButtonSystemItem.Done)
        navigationItem.rightBarButtonItem = nextButton

        val container = UIView()
        val scrollView = container.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.edgesToSuperview()

        val introLabel = Body1Label()
        introLabel.text = texts.get(STRING_INTRO_CREATE_WALLET)

        val mnemonicLabel = Headline2Label()
        mnemonicLabel.text = Mnemonic.generateEnglishMnemonic()

        val mnemonicPadding = UIView(CGRect.Zero())
        mnemonicPadding.addSubview(mnemonicLabel)
        mnemonicLabel.topToSuperview().bottomToSuperview().widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)

        val copyButton = CommonButton(texts.get(STRING_BUTTON_COPY)).apply {
            addOnTouchUpInsideListener { _, _ ->
                presentViewController(buildSensitiveDataCopyDialog(texts, mnemonicLabel.text), true) {}
            }
            fixedWidth(100.0)
        }
        val copyButtonContainer = UIView(CGRect.Zero())
        copyButtonContainer.addSubview(copyButton)
        copyButton.topToSuperview().bottomToSuperview().rightToSuperview()

        val verticalStack = UIStackView(
            NSArray(
                introLabel,
                mnemonicPadding,
                copyButtonContainer
            )
        )
        verticalStack.axis = UILayoutConstraintAxis.Vertical
        verticalStack.spacing = DEFAULT_MARGIN * 2
        container.addSubview(verticalStack)
        verticalStack.topToSuperview(false, DEFAULT_MARGIN * 3)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH).bottomToSuperview()

        container.addSubview(verticalStack)

        nextButton.setOnClickListener {
            navigationController.pushViewController(
                ConfirmCreateWalletViewController(SecretString.create(mnemonicLabel.text)),
                true
            )
        }
    }
}