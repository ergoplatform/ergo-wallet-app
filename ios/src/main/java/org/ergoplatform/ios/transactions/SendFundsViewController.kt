package org.ergoplatform.ios.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.ErgoAmount
import org.ergoplatform.URL_COLD_WALLET_HELP
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.ios.addressbook.ChooseAddressDialogViewController
import org.ergoplatform.ios.tokens.SendTokenEntryView
import org.ergoplatform.ios.ui.Body1BoldLabel
import org.ergoplatform.ios.ui.Body1Label
import org.ergoplatform.ios.ui.CardView
import org.ergoplatform.ios.ui.CommonButton
import org.ergoplatform.ios.ui.DEFAULT_MARGIN
import org.ergoplatform.ios.ui.EndIconTextField
import org.ergoplatform.ios.ui.ErgoAmountView
import org.ergoplatform.ios.ui.FONT_SIZE_BODY1
import org.ergoplatform.ios.ui.FONT_SIZE_HEADLINE1
import org.ergoplatform.ios.ui.IMAGE_ADDRESSBOOK
import org.ergoplatform.ios.ui.IMAGE_EDIT_CIRCLE
import org.ergoplatform.ios.ui.IMAGE_FULL_AMOUNT
import org.ergoplatform.ios.ui.IMAGE_INFORMATION
import org.ergoplatform.ios.ui.IMAGE_PLUS
import org.ergoplatform.ios.ui.IMAGE_QR_SCAN
import org.ergoplatform.ios.ui.IMAGE_SEND
import org.ergoplatform.ios.ui.IMAGE_TX_DONE
import org.ergoplatform.ios.ui.IosStringProvider
import org.ergoplatform.ios.ui.MAX_WIDTH
import org.ergoplatform.ios.ui.OnlyNumericInputTextFieldDelegate
import org.ergoplatform.ios.ui.PrimaryButton
import org.ergoplatform.ios.ui.ProgressViewController
import org.ergoplatform.ios.ui.QrScannerViewController
import org.ergoplatform.ios.ui.TextButton
import org.ergoplatform.ios.ui.animateLayoutChanges
import org.ergoplatform.ios.ui.bottomToKeyboard
import org.ergoplatform.ios.ui.bottomToSuperview
import org.ergoplatform.ios.ui.buildAddressSelectorView
import org.ergoplatform.ios.ui.buildSimpleAlertController
import org.ergoplatform.ios.ui.centerHorizontal
import org.ergoplatform.ios.ui.centerVertical
import org.ergoplatform.ios.ui.centerVerticallyTo
import org.ergoplatform.ios.ui.clearArrangedSubviews
import org.ergoplatform.ios.ui.edgesToSuperview
import org.ergoplatform.ios.ui.fixedHeight
import org.ergoplatform.ios.ui.fixedWidth
import org.ergoplatform.ios.ui.getAppDelegate
import org.ergoplatform.ios.ui.getIosSystemImage
import org.ergoplatform.ios.ui.leftToSuperview
import org.ergoplatform.ios.ui.openStorePage
import org.ergoplatform.ios.ui.rightToSuperview
import org.ergoplatform.ios.ui.runOnMainThread
import org.ergoplatform.ios.ui.setHiddenAnimated
import org.ergoplatform.ios.ui.setHtmlText
import org.ergoplatform.ios.ui.shareText
import org.ergoplatform.ios.ui.topToSuperview
import org.ergoplatform.ios.ui.uiColorErgo
import org.ergoplatform.ios.ui.widthMatchesSuperview
import org.ergoplatform.ios.ui.wrapInVerticalScrollView
import org.ergoplatform.ios.ui.wrapWithTrailingImage
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_BUTTON_DONE
import org.ergoplatform.uilogic.STRING_BUTTON_PLEASE_RATE
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.uilogic.STRING_DESC_PLEASE_RATE
import org.ergoplatform.uilogic.STRING_DESC_SEND_FUNDS
import org.ergoplatform.uilogic.STRING_DESC_TRANSACTION_SEND
import org.ergoplatform.uilogic.STRING_ERROR_TOKEN_AMOUNT
import org.ergoplatform.uilogic.STRING_HINT_AMOUNT_CURRENCY
import org.ergoplatform.uilogic.STRING_HINT_READ_ONLY
import org.ergoplatform.uilogic.STRING_INFO_PURPOSE_MESSAGE
import org.ergoplatform.uilogic.STRING_INFO_PURPOSE_MESSAGE_ACCEPT
import org.ergoplatform.uilogic.STRING_INFO_PURPOSE_MESSAGE_DECLINE
import org.ergoplatform.uilogic.STRING_LABEL_ADD_TOKEN
import org.ergoplatform.uilogic.STRING_LABEL_ALL_ADDRESSES
import org.ergoplatform.uilogic.STRING_LABEL_AMOUNT
import org.ergoplatform.uilogic.STRING_LABEL_PURPOSE
import org.ergoplatform.uilogic.STRING_LABEL_RECEIVER_ADDRESS
import org.ergoplatform.uilogic.STRING_LABEL_SEND_FROM
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_BALANCE
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getNumOfAddresses
import org.ergoplatform.wallet.isReadOnly
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.NSTextAlignment
import org.robovm.apple.uikit.UIAlertAction
import org.robovm.apple.uikit.UIAlertActionStyle
import org.robovm.apple.uikit.UIAlertController
import org.robovm.apple.uikit.UIAlertControllerStyle
import org.robovm.apple.uikit.UIBarButtonItem
import org.robovm.apple.uikit.UIBarButtonItemStyle
import org.robovm.apple.uikit.UIButton
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIControlEvents
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UIFont
import org.robovm.apple.uikit.UIFontWeight
import org.robovm.apple.uikit.UIImageSymbolScale
import org.robovm.apple.uikit.UIImageView
import org.robovm.apple.uikit.UIKeyboardType
import org.robovm.apple.uikit.UILabel
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIReturnKeyType
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UITapGestureRecognizer
import org.robovm.apple.uikit.UITextField
import org.robovm.apple.uikit.UITextFieldDelegateAdapter
import org.robovm.apple.uikit.UITextFieldViewMode
import org.robovm.apple.uikit.UITextView
import org.robovm.apple.uikit.UIView
import org.robovm.apple.uikit.UIViewContentMode

class SendFundsViewController(
    private val walletId: Int,
    private val derivationIdx: Int = -1,
    private val paymentRequest: String? = null
) : SubmitTransactionViewController() {

    override val uiLogic = IosSendFundsUiLogic()
    private lateinit var scrollView: UIView
    private lateinit var walletTitle: UILabel
    private lateinit var addressNameLabel: UILabel
    private lateinit var balanceLabel: UILabel
    private lateinit var otherCurrencyLabel: UILabel
    private lateinit var otherCurrencyContainer: UIView
    private lateinit var readOnlyHint: UITextView
    private lateinit var feeLabel: UILabel
    private lateinit var grossAmountLabel: ErgoAmountView
    private lateinit var tokensUiList: UIStackView
    private lateinit var tokensError: UILabel
    private lateinit var sendButton: UIButton
    private lateinit var addTokenButton: UIButton

    private lateinit var inputReceiver: EndIconTextField
    private var inputReceiverShowsLabel = false
        set(value) {
            field = value
            inputReceiver.clearButtonMode = if (value) UITextFieldViewMode.Always else UITextFieldViewMode.Never
            inputReceiver.rightViewMode = if (value) UITextFieldViewMode.Never else UITextFieldViewMode.Always
        }
    private lateinit var inputMessage: EndIconTextField
    private lateinit var inputAmount: EndIconTextField

    private var txDoneView: UIView? = null

    override fun viewDidLoad() {
        super.viewDidLoad()

        texts = getAppDelegate().texts
        title = texts.get(STRING_BUTTON_SEND)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val uiBarButtonItem = UIBarButtonItem(
            getIosSystemImage(IMAGE_QR_SCAN, UIImageSymbolScale.Small),
            UIBarButtonItemStyle.Plain
        )
        uiBarButtonItem.setOnClickListener {
            presentViewController(QrScannerViewController {
                uiLogic.qrCodeScanned(it, IosStringProvider(texts), { data, walletId ->
                    navigationController.pushViewController(
                        ColdWalletSigningViewController(
                            data,
                            walletId
                        ), true
                    )
                }, { ergoPayRequest ->
                    navigationController.pushViewController(
                        ErgoPaySigningViewController(
                            ergoPayRequest, walletId, uiLogic.derivedAddressIdx ?: -1
                        ), true
                    )
                }, { address, amount, message ->
                    setReceiverText(address)
                    amount?.let { setInputAmount(amount) }
                    message?.let {
                        inputMessage.text = message
                        inputMessage.sendControlEventsActions(UIControlEvents.EditingChanged)
                    }
                })
            }, true) {}
        }
        navigationController.topViewController.navigationItem.rightBarButtonItem = uiBarButtonItem

        walletTitle = Body1Label()
        walletTitle.numberOfLines = 1
        val addressNameContainer = buildAddressSelectorView(this, walletId, true) { onAddressChosen(it) }
        addressNameLabel = addressNameContainer.content
        balanceLabel = Body1Label()
        balanceLabel.numberOfLines = 1

        val introLabel = Body1Label()
        introLabel.text = texts.get(STRING_DESC_SEND_FUNDS)

        readOnlyHint = UITextView(CGRect.Zero()).apply {
            setHtmlText(texts.get(STRING_HINT_READ_ONLY).replace("href=\"\"", "href=\"$URL_COLD_WALLET_HELP\""))
            textAlignment = NSTextAlignment.Center
            layer.borderWidth = 1.0
            layer.cornerRadius = 4.0
            layer.borderColor = uiColorErgo.cgColor
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Semibold)

        }

        inputReceiver = EndIconTextField().apply {
            placeholder = texts.get(STRING_LABEL_RECEIVER_ADDRESS)
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    inputMessage.becomeFirstResponder()
                    return true
                }

                override fun shouldBeginEditing(textField: UITextField?): Boolean {
                    return !inputReceiverShowsLabel && super.shouldBeginEditing(textField)
                }
            }

            addOnEditingChangedListener {
                setHasError(false)
                uiLogic.receiverAddress = text
                if (text.isBlank())
                    inputReceiverShowsLabel = false
                else
                    viewControllerScope.launch {
                        val recipient = text
                        val appDelegate = getAppDelegate()
                        getAddressLabelFromDatabase(
                            appDelegate.database,
                            recipient,
                            IosStringProvider(appDelegate.texts)
                        )?.let {
                            if (text == recipient) runOnMainThread {
                                inputReceiverShowsLabel = true
                                text = it
                                endEditing(true)
                            }
                        }
                    }
            }

            setCustomActionField(
                getIosSystemImage(
                    IMAGE_ADDRESSBOOK,
                    UIImageSymbolScale.Small
                )!!
            ) {
                presentViewController(ChooseAddressDialogViewController { addressWithLabel ->
                    setReceiverText(addressWithLabel.address)
                }, true) {}
            }
        }

        inputMessage = EndIconTextField().apply {
            placeholder = texts.get(STRING_LABEL_PURPOSE)
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    inputAmount.becomeFirstResponder()
                    return true
                }
            }
            addOnEditingChangedListener {
                setHasError(false)
                uiLogic.message = text
            }
            setCustomActionField(
                getIosSystemImage(
                    IMAGE_INFORMATION,
                    UIImageSymbolScale.Small
                )!!
            ) { showPurposeMessageInfoDialog() }
        }

        inputAmount = EndIconTextField().apply {
            keyboardType = UIKeyboardType.NumbersAndPunctuation
            returnKeyType = UIReturnKeyType.Next
            delegate = object : OnlyNumericInputTextFieldDelegate() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    inputAmount.resignFirstResponder()
                    return true
                }
            }
            addOnEditingChangedListener {
                setHasError(false)
                uiLogic.inputAmountChanged(text)
            }
            setCustomActionField(
                getIosSystemImage(
                    IMAGE_FULL_AMOUNT,
                    UIImageSymbolScale.Small
                )!!
            ) { setInputAmount(uiLogic.getMaxPossibleAmountToSend()) }

        }

        otherCurrencyLabel = Body1Label().apply {
            textAlignment = NSTextAlignment.Right
        }
        otherCurrencyContainer = otherCurrencyLabel
            .wrapWithTrailingImage(
                getIosSystemImage(IMAGE_EDIT_CIRCLE, UIImageSymbolScale.Small, 20.0)!!,
                keepWidth = true
            ).apply {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    val changed = uiLogic.switchInputAmountMode()
                    if (changed) {
                        getAppDelegate().prefs.isSendInputFiatAmount = uiLogic.inputIsFiat
                        inputAmount.text = uiLogic.inputAmountString
                        setInputAmountLabel()
                        uiLogic.notifyAmountsChanged()
                    }
                })
                isHidden = true
            }

        feeLabel = Body1Label().apply {
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                presentViewController(
                    ChooseFeeAmountViewController(
                        uiLogic
                    ), true
                ) {}
            })
        }
        grossAmountLabel = ErgoAmountView(true, FONT_SIZE_HEADLINE1)
        val grossAmountContainer = UIView()
        grossAmountContainer.layoutMargins = UIEdgeInsets.Zero()
        grossAmountContainer.addSubview(grossAmountLabel)
        grossAmountLabel.topToSuperview().bottomToSuperview().centerHorizontal()

        tokensUiList = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN
            isHidden = true
        }
        tokensError = Body1Label().apply {
            text = texts.get(STRING_ERROR_TOKEN_AMOUNT)
            isHidden = true
            textAlignment = NSTextAlignment.Center
            textColor = uiColorErgo
        }

        sendButton = PrimaryButton(
            texts.get(STRING_BUTTON_SEND),
            getIosSystemImage(IMAGE_SEND, UIImageSymbolScale.Small)
        )
        sendButton.addOnTouchUpInsideListener { _, _ -> checkAndStartPayment() }

        addTokenButton = CommonButton(
            texts.get(STRING_LABEL_ADD_TOKEN), getIosSystemImage(
                IMAGE_PLUS, UIImageSymbolScale.Small
            )
        )
        addTokenButton.isHidden = true
        addTokenButton.addOnTouchUpInsideListener { _, _ ->
            presentViewController(
                ChooseTokenListViewController(uiLogic) { tokenToAdd ->
                    tokensUiList.superview.animateLayoutChanges {
                        uiLogic.newTokenChosen(tokenToAdd)
                    }
                }, true
            ) {}
        }

        val buttonContainer = UIView()
        buttonContainer.addSubview(sendButton)
        buttonContainer.addSubview(addTokenButton)
        sendButton.topToSuperview().bottomToSuperview().rightToSuperview().fixedWidth(120.0)
        addTokenButton.centerVerticallyTo(sendButton).leftToSuperview().fixedWidth(120.0)

        val container = UIView()
        val stackView = UIStackView(
            NSArray(
                walletTitle,
                addressNameContainer,
                balanceLabel,
                readOnlyHint,
                introLabel,
                inputReceiver,
                inputMessage,
                inputAmount,
                otherCurrencyContainer,
                feeLabel,
                grossAmountContainer,
                tokensUiList,
                tokensError,
                buttonContainer
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.spacing = 2 * DEFAULT_MARGIN
        stackView.setCustomSpacing(0.0, walletTitle)
        stackView.setCustomSpacing(0.0, addressNameContainer)
        stackView.setCustomSpacing(0.0, inputAmount)
        scrollView = container.wrapInVerticalScrollView()
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.topToSuperview().widthMatchesSuperview().bottomToKeyboard(this)
        scrollView.isHidden = true

        if (getAppDelegate().prefs.isSendInputFiatAmount != uiLogic.inputIsFiat) {
            uiLogic.switchInputAmountMode()
        }
        setInputAmountLabel()
    }

    private fun setReceiverText(address: String) {
        inputReceiver.text = address
        inputReceiver.sendControlEventsActions(UIControlEvents.EditingChanged)
    }

    private fun showPurposeMessageInfoDialog(startPayment: Boolean = false) {
        val uac = UIAlertController("", texts.get(STRING_INFO_PURPOSE_MESSAGE), UIAlertControllerStyle.Alert)
        val prefs = getAppDelegate().prefs
        uac.addAction(
            UIAlertAction(
                texts.get(STRING_INFO_PURPOSE_MESSAGE_ACCEPT),
                UIAlertActionStyle.Default
            ) {
                prefs.sendTxMessages = true
                if (startPayment) checkAndStartPayment()
            })
        uac.addAction(
            UIAlertAction(
                texts.get(STRING_INFO_PURPOSE_MESSAGE_DECLINE),
                UIAlertActionStyle.Default
            ) {
                prefs.sendTxMessages = false
            })
        presentViewController(uac, true) {}
    }

    private fun setInputAmount(amountToSend: ErgoAmount) {
        uiLogic.setAmountToSendErg(amountToSend)
        inputAmount.text = uiLogic.inputAmountString
    }

    private fun setInputAmountLabel() {
        inputAmount.placeholder =
            if (uiLogic.inputIsFiat)
                texts.format(
                    STRING_HINT_AMOUNT_CURRENCY,
                    WalletStateSyncManager.getInstance().fiatCurrency.uppercase()
                )
            else texts.get(STRING_LABEL_AMOUNT)
    }


    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        val appDelegate = getAppDelegate()
        uiLogic.initWallet(
            appDelegate.database,
            ApiServiceManager.getOrInit(appDelegate.prefs),
            walletId,
            derivationIdx,
            paymentRequest
        )

        inputReceiver.text = uiLogic.receiverAddress
        inputMessage.text = uiLogic.message
        if (uiLogic.amountToSend.nanoErgs > 0) setInputAmount(uiLogic.amountToSend)
    }

    private fun checkAndStartPayment() {
        // auto correct could have changed the message after the last change event
        uiLogic.message = inputMessage.text

        val checkResponse = uiLogic.checkCanMakePayment(getAppDelegate().prefs)

        inputReceiver.setHasError(checkResponse.receiverError)
        // this might have messed with a label, restore
        if (inputReceiverShowsLabel)
            inputReceiver.rightViewMode = UITextFieldViewMode.Never
        inputAmount.setHasError(checkResponse.amountError)
        inputMessage.setHasError(checkResponse.messageError)
        if (checkResponse.receiverError) {
            inputReceiver.becomeFirstResponder()
        } else if (checkResponse.amountError) {
            inputAmount.becomeFirstResponder()
        }
        if (checkResponse.tokenError) {
            tokensError.setHiddenAnimated(false)
            setFocusToEmptyTokenAmountInput()
        }
        if (checkResponse.messageError) {
            showPurposeMessageInfoDialog(true)
        }

        if (checkResponse.canPay) {
            if (uiLogic.wallet!!.walletConfig.isReadOnly() == false)
                uiLogic.prepareTransactionForSigning(
                    getAppDelegate().prefs,
                    IosStringProvider(texts)
                )
            else
                startPayment()
        }
    }

    private fun setFocusToEmptyTokenAmountInput() {
        (tokensUiList.arrangedSubviews.firstOrNull { (it as? SendTokenEntryView)?.hasAmount() == false }
                as? SendTokenEntryView)?.setFocus()
    }

    inner class IosSendFundsUiLogic : SendFundsUiLogic() {
        var feeSuggestionObserver: (() -> Unit)? = null

        private val progressViewController =
            ProgressViewController.ProgressViewControllerPresenter(this@SendFundsViewController)

        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun notifyWalletStateLoaded() {
            runOnMainThread {
                // if we already have a successful tx, there is no need to refresh the ui
                if (txDoneView == null) {
                    walletTitle.text = texts.format(STRING_LABEL_SEND_FROM, wallet!!.walletConfig.displayName)
                    readOnlyHint.isHidden = !uiLogic.wallet!!.isReadOnly()
                    scrollView.isHidden = false
                }
            }
        }

        override fun notifyDerivedAddressChanged() {
            runOnMainThread {
                addressNameLabel.text = derivedAddress?.getAddressLabel(IosStringProvider(texts))
                    ?: texts.format(STRING_LABEL_ALL_ADDRESSES, wallet?.getNumOfAddresses())
            }
        }

        override fun notifyTokensChosenChanged() {
            runOnMainThread {
                addTokenButton.isHidden = (uiLogic.tokensChosen.size >= uiLogic.tokensAvail.size)
                tokensUiList.clearArrangedSubviews()
                tokensError.isHidden = true
                val walletStateSyncManager = WalletStateSyncManager.getInstance()
                uiLogic.tokensChosen.forEach {
                    val tokenId = it.key
                    tokensAvail[tokenId]?.let { tokenEntity ->
                        val tokenEntry =
                            SendTokenEntryView(
                                uiLogic,
                                tokensError,
                                tokenEntity,
                                it.value,
                                texts,
                                walletStateSyncManager.getTokenPrice(tokenEntity.tokenId)
                            )
                        tokensUiList.addArrangedSubview(tokenEntry)
                    }
                }
                tokensUiList.isHidden = uiLogic.tokensChosen.isEmpty()
                setFocusToEmptyTokenAmountInput()

                uiLogic.getPaymentRequestWarnings(IosStringProvider(texts))?.let {
                    val uac = buildSimpleAlertController("", it, texts)
                    presentViewController(uac, true) {}

                }
            }
        }

        override fun onNotifySuggestedFees() {
            feeSuggestionObserver?.invoke()
        }

        override fun notifyAmountsChanged() {
            runOnMainThread {
                val text = IosStringProvider(texts)
                feeLabel.text = getFeeDescriptionLabel(text)
                grossAmountLabel.setErgoAmount(grossAmount)
                val otherCurrency = uiLogic.getOtherCurrencyLabel(text)
                otherCurrencyContainer.isHidden = (otherCurrency == null)
                otherCurrencyLabel.text = otherCurrency
            }
        }

        override fun notifyBalanceChanged() {
            runOnMainThread {
                balanceLabel.text = texts.format(STRING_LABEL_WALLET_BALANCE, balance.toStringRoundToDecimals())
            }
        }

        override fun notifyUiLocked(locked: Boolean) {
            runOnMainThread {
                progressViewController.setUiLocked(locked)
            }
        }

        override fun notifyHasTxId(txId: String) {
            LogUtils.logDebug("SendFunds", "Success, tx id $txId")

            runOnMainThread {
                scrollView.isHidden = true
                val txDoneCardView = CardView()
                view.addSubview(txDoneCardView)
                val texts = getAppDelegate().texts

                val image = UIImageView(getIosSystemImage(IMAGE_TX_DONE, UIImageSymbolScale.Large))
                image.contentMode = UIViewContentMode.ScaleAspectFit
                image.tintColor = uiColorErgo
                image.fixedHeight(100.0)

                val descLabel = Body1Label()
                descLabel.text = texts.get(STRING_DESC_TRANSACTION_SEND)
                descLabel.textAlignment = NSTextAlignment.Center
                val txLabel = Body1BoldLabel()
                txLabel.text = txId
                txLabel.textAlignment = NSTextAlignment.Center
                txLabel.isUserInteractionEnabled = true
                txLabel.addGestureRecognizer(UITapGestureRecognizer {
                    shareText(getExplorerTxUrl(txId), txLabel)
                })

                val doneButton = PrimaryButton(texts.get(STRING_BUTTON_DONE))
                doneButton.addOnTouchUpInsideListener { _, _ ->
                    navigationController.popViewController(true)
                }
                val doneButtonContainer = UIView()
                doneButtonContainer.addSubview(doneButton)
                doneButton.centerHorizontal().topToSuperview().bottomToSuperview().fixedWidth(150.0)

                val rateLabel = Body1Label().apply {
                    text = texts.get(STRING_DESC_PLEASE_RATE)
                    textAlignment = NSTextAlignment.Center
                }
                val rateButton = TextButton(texts.get(STRING_BUTTON_PLEASE_RATE))
                rateButton.addOnTouchUpInsideListener { _, _ ->
                    openStorePage()
                }

                val txDoneStack =
                    UIStackView(NSArray(image, descLabel, txLabel, doneButtonContainer, rateLabel, rateButton))
                txDoneStack.axis = UILayoutConstraintAxis.Vertical
                txDoneStack.spacing = DEFAULT_MARGIN * 3
                txDoneStack.setCustomSpacing(0.0, rateLabel)

                txDoneCardView.contentView.addSubview(txDoneStack)
                txDoneStack.edgesToSuperview(inset = DEFAULT_MARGIN * 2)

                txDoneCardView.centerVertical().widthMatchesSuperview(inset = DEFAULT_MARGIN * 2, maxWidth = MAX_WIDTH)
                txDoneView = txDoneCardView
            }
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            if (!txResult.success) {
                runOnMainThread {
                    showTxResultError(txResult)
                }
            }
        }

        override fun notifyHasPreparedTx(preparedTx: TransactionInfo) {
            runOnMainThread {
                presentViewController(
                    ConfirmSendFundsDialogViewController(preparedTx) {
                        startPayment()
                    },
                    true
                ) {}
            }
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            runOnMainThread {
                showSigningPromptVc(signingPrompt)
            }

        }

        override fun showErrorMessage(message: String) {
            runOnMainThread {
                val vc = buildSimpleAlertController("", message, texts)
                presentViewController(vc, true) {}
            }
        }
    }
}