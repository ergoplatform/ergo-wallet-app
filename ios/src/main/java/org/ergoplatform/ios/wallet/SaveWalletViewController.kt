package org.ergoplatform.ios.wallet

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.ios.api.IosAuthentication
import org.ergoplatform.ios.api.IosEncryptionManager
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.SaveWalletUiLogic
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.uikit.*

class SaveWalletViewController(
    private val mnemonic: SecretString,
    private val fromRestore: Boolean
) : ViewControllerWithKeyboardLayoutGuide() {
    private lateinit var progressIndicator: UIActivityIndicatorView
    private lateinit var scrollView: UIScrollView
    private lateinit var addressLabel: UILabel
    private lateinit var nameInputField: UITextField
    private lateinit var labelDisplayName: Body1BoldLabel
    private lateinit var buttonAltAddress: UIButton
    private lateinit var uiLogic: SaveWalletUiLogic
    private lateinit var addressInfoLabel: UILabel

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_TITLE_WALLET_DETAILS)
        view.backgroundColor = UIColor.systemBackground()

        navigationController.navigationBar?.tintColor = uiColorErgo

        val container = UIView()
        scrollView = container.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.widthMatchesSuperview().topToSuperview().bottomToKeyboard(this)

        val introLabel = Body1Label()
        introLabel.text = texts.get(STRING_INTRO_SAVE_WALLET)

        addressLabel = Headline2Label()

        addressInfoLabel = Body1Label()
        addressInfoLabel.text = texts.get(STRING_INTRO_SAVE_WALLET2)
        val saveInfoLabel = Body1Label().apply {
            text = texts.get(STRING_INTRO_SAVE_WALLET3)
        }

        labelDisplayName = Body1BoldLabel().apply {
            textColor = uiColorErgo
            text = texts.get(STRING_LABEL_WALLET_NAME)
        }
        nameInputField = createTextField().apply {
            clearButtonMode = UITextFieldViewMode.Always
            returnKeyType = UIReturnKeyType.Done
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    textField?.resignFirstResponder()
                    return true
                }
            }
        }

        buttonAltAddress = TextButton(texts.get(STRING_BUTTON_ALT_ADDRESS))
        buttonAltAddress.addOnTouchUpInsideListener { _, _ ->
            uiLogic.switchAddress()
            viewControllerScope.launch(Dispatchers.IO) {
                refreshAddressInfo()
            }
        }

        val buttonSavePassword = TextButton(texts.get(STRING_BUTTON_SAVE_PASSWORD_ENCRYPTED))
        buttonSavePassword.addOnTouchUpInsideListener { _, _ ->
            PasswordViewController.showDialog(
                this@SaveWalletViewController,
                { password ->
                    if (uiLogic.isPasswordWeak(password)) {
                        return@showDialog texts.get(STRING_ERR_PASSWORD)
                    }

                    val encrypted = AesEncryptionManager.encryptData(
                        password,
                        uiLogic.signingSecrets.toBytes()
                    )

                    saveToDbAndDismissController(ENC_TYPE_PASSWORD, encrypted)
                    return@showDialog null
                }, true
            )
        }

        val savePwInfoLabel =
            Body1Label().apply { text = texts.get(STRING_DESC_SAVE_PASSWORD_ENCRYPTED) }

        val buttonSaveKeychain = TextButton(texts.get(STRING_BUTTON_SAVE_KEYCHAIN))
        buttonSaveKeychain.isEnabled = IosAuthentication.canAuthenticate()
        buttonSaveKeychain.addOnTouchUpInsideListener { _, _ ->
            IosAuthentication.authenticate(
                texts.get(STRING_TITLE_AUTHENTICATE),
                object : IosAuthentication.IosAuthCallback {
                    override fun onAuthenticationSucceeded(context: LAContext) {
                        try {
                            val encrypted = IosEncryptionManager.encryptDataWithKeychain(
                                uiLogic.signingSecrets.toBytes(),
                                context
                            )

                            runOnMainThread {
                                saveToDbAndDismissController(
                                    ENC_TYPE_DEVICE,
                                    encrypted
                                )
                            }
                        } catch (t: Throwable) {
                            LogUtils.logDebug("KeychainEnc", "Error encrypting mnemonic", t)
                            runOnMainThread {
                                val uac = UIAlertController(
                                    texts.format(STRING_ERROR_DEVICE_SECURITY, ""),
                                    t.message, UIAlertControllerStyle.Alert
                                )
                                uac.addAction(
                                    UIAlertAction(
                                        texts.get(STRING_ZXING_BUTTON_OK),
                                        UIAlertActionStyle.Default
                                    ) {})
                                presentViewController(uac, true) {}
                            }
                        }
                    }

                    override fun onAuthenticationError(error: String) {
                        runOnMainThread {
                            presentViewController(
                                buildSimpleAlertController(
                                    texts.format(STRING_ERROR_DEVICE_SECURITY, ""),
                                    error, texts
                                ), true
                            ) {}
                        }
                    }

                    override fun onAuthenticationCancelled() {
                        // do nothing
                    }

                })
        }

        val saveKeychainInfo =
            Body1Label().apply { text = texts.get(STRING_DESC_SAVE_KEYCHAIN) }

        val addressInfoStack = UIStackView(
            NSArray(
                introLabel,
                addressLabel,
                buttonAltAddress,
                addressInfoLabel,
                saveInfoLabel,
                labelDisplayName,
                nameInputField,
                createHorizontalSeparator(),
                buttonSavePassword,
                savePwInfoLabel,
                createHorizontalSeparator(),
                buttonSaveKeychain,
                saveKeychainInfo
            )
        )
        addressInfoStack.axis = UILayoutConstraintAxis.Vertical
        addressInfoStack.spacing = DEFAULT_MARGIN * 2
        addressInfoStack.arrangedSubviews.forEach { it.widthMatchesSuperview() }
        container.addSubview(addressInfoStack)
        addressInfoStack.setCustomSpacing(DEFAULT_MARGIN * 4, addressInfoLabel)
        addressInfoStack.setCustomSpacing(DEFAULT_MARGIN * 4, saveInfoLabel)
        addressInfoStack.setCustomSpacing(DEFAULT_MARGIN, labelDisplayName)
        addressInfoStack.setCustomSpacing(DEFAULT_MARGIN * 4, nameInputField)
        addressInfoStack.topToSuperview(false, DEFAULT_MARGIN * 2)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH).bottomToSuperview()

        container.addSubview(addressInfoStack)

        progressIndicator = UIActivityIndicatorView()
        progressIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.Large
        view.addSubview(progressIndicator)
        progressIndicator.centerVertical().centerHorizontal()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveToDbAndDismissController(encType: Int, secretStorage: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            val appDelegate = getAppDelegate()
            uiLogic.suspendSaveToDb(
                appDelegate.database.walletDbProvider, nameInputField.text,
                encType, secretStorage
            )
        }
        navigationController.dismissViewController(true) {
            uiLogic.eraseSecrets()
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        calculatePkAddressAndFillView()
    }

    private fun calculatePkAddressAndFillView() {
        scrollView.isHidden = true
        progressIndicator.startAnimating()

        viewControllerScope.launch(Dispatchers.IO) {
            uiLogic = SaveWalletUiLogic(mnemonic, fromRestore)
            refreshAddressInfo()
        }
    }

    private suspend fun refreshAddressInfo() {
        val publicErgoAddressFromMnemonic = uiLogic.publicAddress
        val appDelegate = getAppDelegate()
        val db = appDelegate.database.walletDbProvider
        val walletDisplayName =
            uiLogic.getSuggestedDisplayName(db, IosStringProvider(appDelegate.texts))
        val showDisplayName = uiLogic.showSuggestedDisplayName(db)

        runOnMainThread {
            addressLabel.text = publicErgoAddressFromMnemonic
            nameInputField.text = walletDisplayName
            nameInputField.isHidden = !showDisplayName
            labelDisplayName.isHidden = !showDisplayName
            progressIndicator.isHidden = true
            scrollView.isHidden = false
            buttonAltAddress.isHidden = !uiLogic.hasAlternativeAddress
        }

        uiLogic.startDerivedAddressesSearch(
            ApiServiceManager.getOrInit(appDelegate.prefs),
            db
        ) { num ->
            runOnMainThread {
                addressInfoLabel.text = if (num == 0) appDelegate.texts.get(STRING_INTRO_SAVE_WALLET2)
                else appDelegate.texts.format(STRING_INTRO_SAVE_WALLET_DERIVED_ADDRESSES_NUM, (num + 1).toString())
            }
        }
    }
}