package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.getSerializedXpubKeyFromMnemonic
import org.ergoplatform.ios.api.IosEncryptionManager
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.addresses.WalletAddressesViewController
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.WalletConfigUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class WalletConfigViewController(private val walletId: Int) : ViewControllerWithKeyboardLayoutGuide() {

    private lateinit var addressLabel: UILabel
    private lateinit var nameInputField: UITextField
    private lateinit var nameChangeApplyButton: UIButton
    private lateinit var addressesButton: UIButton
    private lateinit var displaySecretsButton: UIButton
    private lateinit var displayXpubButton: UIButton
    private val uiLogic = IosWalletConfigUiLogic()

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_TITLE_WALLET_DETAILS)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        addressLabel = Headline2Label()
        addressLabel.isUserInteractionEnabled = true
        addressLabel.textAlignment = NSTextAlignment.Center
        addressLabel.addGestureRecognizer(UITapGestureRecognizer {
            shareText(addressLabel.text, addressLabel)
        })

        val nameInputLabel = Body1Label()
        nameInputLabel.text = texts.get(STRING_LABEL_WALLET_NAME)

        nameInputField = createTextField()
        nameInputField.returnKeyType = UIReturnKeyType.Done
        nameInputField.clearButtonMode = UITextFieldViewMode.Always
        nameInputField.delegate = object : UITextFieldDelegateAdapter() {
            override fun shouldReturn(textField: UITextField?): Boolean {
                doSaveWalletName()
                return true
            }
        }
        nameInputField.addOnEditingChangedListener { nameChangeApplyButton.isEnabled = true }

        val nameChangeApplyButtonContainer = UIView(CGRect.Zero())
        nameChangeApplyButton = TextButton(texts.get(STRING_BUTTON_APPLY))
        nameChangeApplyButtonContainer.addSubview(nameChangeApplyButton)
        nameChangeApplyButton.topToSuperview().bottomToSuperview().rightToSuperview()
        nameChangeApplyButton.addOnTouchUpInsideListener { _, _ -> doSaveWalletName() }

        val deleteButton = UIBarButtonItem(UIBarButtonSystemItem.Trash)
        deleteButton.setOnClickListener {
            onDeleteClicked(texts, deleteButton)
        }
        navigationController.topViewController.navigationItem.rightBarButtonItem = deleteButton

        val descAddresses = Body1Label().apply {
            text = texts.get(STRING_DESC_WALLET_ADDRESSES)
        }
        addressesButton = TextButton(texts.get(STRING_TITLE_WALLET_ADDRESSES))
        addressesButton.addOnTouchUpInsideListener { _, _ ->
            navigationController.pushViewController(WalletAddressesViewController(walletId), true)
        }

        val descShowXpub = Body1Label()
        descShowXpub.text = texts.get(STRING_DESC_DISPLAY_XPUBKEY)
        displayXpubButton = TextButton(texts.get(STRING_BUTTON_DISPLAY_XPUBKEY))
        displayXpubButton.addOnTouchUpInsideListener { _, _ -> onDisplayXpubClicked() }

        val descShowSecrets = Body1Label()
        descShowSecrets.text = texts.get(STRING_DESC_DISPLAY_MNEMONIC)
        displaySecretsButton = TextButton(texts.get(STRING_BUTTON_DISPLAY_MNEMONIC))
        displaySecretsButton.addOnTouchUpInsideListener { _, _ -> onDisplaySecretsClicked() }

        val container = UIView()
        val stackView = UIStackView(
            NSArray(
                addressLabel,
                createHorizontalSeparator(),
                nameInputLabel,
                nameInputField,
                nameChangeApplyButtonContainer,
                createHorizontalSeparator(),
                descAddresses,
                addressesButton,
                createHorizontalSeparator(),
                descShowXpub,
                displayXpubButton,
                createHorizontalSeparator(),
                descShowSecrets,
                displaySecretsButton
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.spacing = DEFAULT_MARGIN * 3
        stackView.setCustomSpacing(DEFAULT_MARGIN, nameInputLabel)
        stackView.setCustomSpacing(0.0, nameInputField)
        stackView.setCustomSpacing(DEFAULT_MARGIN, descAddresses)
        stackView.setCustomSpacing(DEFAULT_MARGIN, descShowXpub)
        stackView.setCustomSpacing(DEFAULT_MARGIN, descShowSecrets)
        val scrollView = container.wrapInVerticalScrollView()
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
            uiLogic.initForWallet(walletId, getAppDelegate().database)
        }
    }

    private fun onDeleteClicked(
        texts: I18NBundle,
        deleteButton: UIBarButtonItem
    ) {
        val vc = UIAlertController(
            texts.get(STRING_BUTTON_DELETE),
            texts.get(STRING_LABEL_CONFIRM_DELETE),
            UIAlertControllerStyle.ActionSheet
        )

        val cancel = UIAlertAction(texts.get(STRING_LABEL_CANCEL), UIAlertActionStyle.Cancel) {}
        vc.addAction(cancel)

        val delete = UIAlertAction(texts.get(STRING_BUTTON_DELETE), UIAlertActionStyle.Destructive) {
            doDeleteWallet()
        }
        vc.addAction(delete)
        vc.popoverPresentationController?.barButtonItem = deleteButton

        presentViewController(vc, true) {}
    }

    private fun onDisplayXpubClicked() {
        uiLogic.wallet?.secretStorage?.let {
            startAuthFlow(uiLogic.wallet!!) { mnemonic ->
                displayXpub(getSerializedXpubKeyFromMnemonic(mnemonic))
            }
        } ?: uiLogic.wallet?.extendedPublicKey?.let {
            displayXpub(it)
        }
    }

    private fun displayXpub(xpub: String) {
        presentViewController(ShareWithQrCodeViewController(xpub), true) {}
    }

    private fun onDisplaySecretsClicked() {
        uiLogic.wallet?.let {
            startAuthFlow(it) { mnemonic ->
                val texts = getAppDelegate().texts
                val alert =
                    UIAlertController(texts.get(STRING_BUTTON_DISPLAY_MNEMONIC), mnemonic, UIAlertControllerStyle.Alert)
                alert.addAction(UIAlertAction(texts.get(STRING_BUTTON_COPY), UIAlertActionStyle.Default) {
                    presentViewController(buildSensitiveDataCopyDialog(texts, mnemonic), true) {}
                })
                alert.addAction(UIAlertAction(texts.get(STRING_ZXING_BUTTON_OK), UIAlertActionStyle.Cancel) {})

                presentViewController(alert, true) {}
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun doDeleteWallet() {
        // we use GlobalScope here to not cancel the transaction when we leave this view
        uiLogic.wallet?.let {
            GlobalScope.launch {
                val database = getAppDelegate().database
                database.deleteAllWalletData(it)

                // After we deleted a keychain encrypted wallet, we can prune the keychain data if it is not needed
                if (it.encryptionType == ENC_TYPE_DEVICE &&
                    database.getAllWalletConfigsSynchronous().none { it.encryptionType == ENC_TYPE_DEVICE }
                ) {
                    IosEncryptionManager.emptyKeychain()
                }

            }
        }
        navigationController.popViewController(true)
    }

    private fun doSaveWalletName() {
        nameInputField.text?.let {
            viewControllerScope.launch(Dispatchers.IO) {
                uiLogic.saveChanges(getAppDelegate().database, it)
                runOnMainThread {
                    nameInputField.resignFirstResponder()
                }
            }
        }
    }

    inner class IosWalletConfigUiLogic: WalletConfigUiLogic() {
        override fun onConfigChanged(value: WalletConfig?) {
            value?.let { walletConfig ->
                runOnMainThread {
                    nameInputField.text = walletConfig.displayName
                    addressLabel.text = walletConfig.firstAddress
                    nameChangeApplyButton.isEnabled = false
                    displaySecretsButton.isEnabled = walletConfig.secretStorage != null
                    displayXpubButton.isEnabled =
                        walletConfig.extendedPublicKey != null || walletConfig.secretStorage != null
                }
            }
        }
    }
}