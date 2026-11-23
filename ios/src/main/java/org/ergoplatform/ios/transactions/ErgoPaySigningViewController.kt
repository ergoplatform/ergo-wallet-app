package org.ergoplatform.ios.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.ios.tokens.TokenInformationViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.ChooseWalletViewController
import org.ergoplatform.ios.wallet.addresses.ChooseAddressListDialogViewController
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getNumOfAddresses
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ErgoPaySigningViewController(
    private val request: String,
    private val walletId: Int = -1,
    private val derivationIndex: Int = -1,
    private val doOnComplete: (() -> Unit)? = null
) : SubmitTransactionViewController() {

    override val uiLogic = IosErgoPaySigningUiLogic()

    private val addressChooserContainer = AddressChooserContainer()
    private lateinit var fetchingContainer: FetchDataContainer
    private lateinit var transactionContainer: TransactionWithHeaderContainer
    private lateinit var reloadNavBarItem: UIBarButtonItem
    private val stateDoneContainer = StateDoneContainer()
    private val walletAddressLabel = Body2BoldLabel().apply {
        numberOfLines = 1
        textColor = uiColorErgo
        textAlignment = NSTextAlignment.Center
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        val appDelegate = getAppDelegate()
        texts = appDelegate.texts
        fetchingContainer = FetchDataContainer(IosStringProvider(texts))
        transactionContainer = TransactionWithHeaderContainer()

        title = texts.get(STRING_TITLE_ERGO_PAY_REQUEST)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        reloadNavBarItem =
            UIBarButtonItem(
                getIosSystemImage(IMAGE_RELOAD, UIImageSymbolScale.Small, 20.0),
                UIBarButtonItemStyle.Plain
            )
        navigationItem.rightBarButtonItem = reloadNavBarItem
        reloadNavBarItem.tintColor = UIColor.label()
        reloadNavBarItem.setOnClickListener {
            startReloadFromDapp()
        }
        reloadNavBarItem.isEnabled = false

        view.layoutMargins = UIEdgeInsets.Zero()
        view.addSubview(walletAddressLabel)


        walletAddressLabel.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN)

        val scrollingContainer = UIView(CGRect.Zero())
        val scrollView = scrollingContainer.wrapInVerticalScrollView()

        view.addSubview(scrollView)
        view.addSubview(fetchingContainer)
        view.addSubview(stateDoneContainer)
        view.addSubview(addressChooserContainer)
        scrollingContainer.addSubview(transactionContainer)

        addressChooserContainer.widthMatchesSuperview(maxWidth = MAX_WIDTH).centerVertical()
        fetchingContainer.widthMatchesSuperview(maxWidth = MAX_WIDTH).centerVertical()
        transactionContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        stateDoneContainer.centerVertical().widthMatchesSuperview(maxWidth = MAX_WIDTH)
        scrollView.topToBottomOf(walletAddressLabel, DEFAULT_MARGIN).widthMatchesSuperview()
            .bottomToSuperview()

        uiLogic.init(
            request,
            walletId,
            derivationIndex,
            appDelegate.database,
            appDelegate.prefs,
            IosStringProvider(texts)
        )
    }

    private fun startReloadFromDapp() {
        val appDelegate = getAppDelegate()
        uiLogic.reloadFromDapp(
            appDelegate.prefs,
            IosStringProvider(appDelegate.texts),
            appDelegate.database
        )
    }

    private fun onWalletChosen(wallet: WalletConfig) {
        val appDelegate = getAppDelegate()
        uiLogic.setWalletId(
            wallet.id,
            appDelegate.prefs,
            IosStringProvider(appDelegate.texts),
            appDelegate.database
        )
    }

    override fun onAddressChosen(it: Int?) {
        super.onAddressChosen(it)
        // redo the request - can't be done within uilogic because context is needed on Android
        val appDelegate = getAppDelegate()
        uiLogic.derivedAddressIdChanged(
            appDelegate.prefs,
            IosStringProvider(appDelegate.texts),
            appDelegate.database
        )
    }

    private fun refreshUserInterface(state: ErgoPaySigningUiLogic.State) {
        reloadNavBarItem.isEnabled = uiLogic.canReloadFromDapp()
        addressChooserContainer.isHidden = state != ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS &&
                state != ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET
        fetchingContainer.isHidden = state != ErgoPaySigningUiLogic.State.FETCH_DATA
        transactionContainer.isHidden = state != ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION
        stateDoneContainer.isHidden = state != ErgoPaySigningUiLogic.State.DONE

        when (state) {
            ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> {
                addressChooserContainer.populateWaitForAddressView(state)
            }
            ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET -> {
                addressChooserContainer.populateWaitForAddressView(state)
            }
            ErgoPaySigningUiLogic.State.FETCH_DATA -> {
                // nothing to do
            }
            ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> transactionContainer.showTransactionInfo()
            ErgoPaySigningUiLogic.State.DONE -> stateDoneContainer.showDoneInfo()
        }
    }

    inner class AddressChooserContainer : CardView() {
        private var label: Body1Label? = null
        private var button: PrimaryButton? = null

        fun populateWaitForAddressView(state: ErgoPaySigningUiLogic.State) {
            if (contentView.subviews.isEmpty()) {
                val image = UIImageView(ergoLogoImage.imageWithTintColor(UIColor.label())).apply {
                    fixedHeight(100.0)
                    contentMode = UIViewContentMode.ScaleAspectFit
                }

                val label = Body1Label().apply {
                    textAlignment = NSTextAlignment.Center
                }

                val button = PrimaryButton(texts.get(STRING_TITLE_CHOOSE_ADDRESS)).apply {
                    addOnTouchUpInsideListener { _, _ ->
                        showAddressOrWalletChooser()
                    }
                }

                contentView.apply {
                    addSubview(image)
                    addSubview(label)
                    addSubview(button)

                    image.topToSuperview(topInset = DEFAULT_MARGIN * 2).centerHorizontal()
                    label.topToBottomOf(image, DEFAULT_MARGIN * 3)
                        .widthMatchesSuperview(inset = DEFAULT_MARGIN)
                    button.topToBottomOf(label, DEFAULT_MARGIN * 2).centerHorizontal()
                        .bottomToSuperview(bottomInset = DEFAULT_MARGIN * 2)
                }

                this.button = button
                this.label = label
            }

            when (state) {
                ErgoPaySigningUiLogic.State.WAIT_FOR_WALLET -> {
                    label?.text = texts.get(STRING_LABEL_ERGO_PAY_CHOOSE_WALLET)
                    button?.setTitle(texts.get(STRING_TITLE_CHOOSE_WALLET), UIControlState.Normal)
                }
                ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> {
                    label?.text = texts.get(STRING_LABEL_ERGO_PAY_CHOOSE_ADDRESS)
                    button?.setTitle(texts.get(STRING_TITLE_CHOOSE_ADDRESS), UIControlState.Normal)
                }
                else -> {}
            }
        }
    }

    private fun showAddressOrWalletChooser() {
        presentViewController(
            if (uiLogic.wallet != null) {
                ChooseAddressListDialogViewController(
                    uiLogic.wallet!!.walletConfig.id,
                    uiLogic.addressRequestCanHandleMultiple
                ) {
                    onAddressChosen(it)
                }
            } else {
                ChooseWalletViewController { onWalletChosen(it) }
            }, true
        ) {}
    }

    private inner class StateDoneContainer : CardView() {
        private var descLabel: Body1Label? = null
        private var rateLabel: Body1Label? = null
        private var image: UIImageView? = null
        private var dismissButton: PrimaryButton? = null
        private var rateButton: TextButton? = null
        private var dismissShouldRetry = false

        fun showDoneInfo() {
            if (contentView.subviews.isEmpty()) {
                val image = UIImageView().apply {
                    contentMode = UIViewContentMode.ScaleAspectFit
                    tintColor = uiColorErgo
                    fixedHeight(100.0)
                }

                val descLabel = Body1Label()
                descLabel.textAlignment = NSTextAlignment.Center

                val dismissButton = PrimaryButton(texts.get(STRING_BUTTON_DONE))
                dismissButton.addOnTouchUpInsideListener { _, _ ->
                    if (dismissShouldRetry) {
                        startReloadFromDapp()
                    } else {
                        navigationController.popViewController(true)
                    }
                }
                val doneButtonContainer = UIView()
                doneButtonContainer.addSubview(dismissButton)
                dismissButton.centerHorizontal().topToSuperview().bottomToSuperview()
                    .fixedWidth(150.0)

                rateLabel = Body1Label().apply {
                    text = texts.get(STRING_DESC_PLEASE_RATE)
                    textAlignment = NSTextAlignment.Center
                }
                rateButton = TextButton(texts.get(STRING_BUTTON_PLEASE_RATE))
                rateButton?.addOnTouchUpInsideListener { _, _ ->
                    openStorePage()
                }

                val txDoneStack = UIStackView().apply {
                    axis = UILayoutConstraintAxis.Vertical
                    spacing = DEFAULT_MARGIN * 3

                    addArrangedSubview(image)
                    addArrangedSubview(descLabel)
                    addArrangedSubview(doneButtonContainer)
                    addArrangedSubview(rateLabel)
                    addArrangedSubview(rateButton)
                    setCustomSpacing(0.0, rateLabel)
                }

                contentView.addSubview(txDoneStack)
                txDoneStack.edgesToSuperview(inset = DEFAULT_MARGIN * 2)

                this.descLabel = descLabel
                this.dismissButton = dismissButton
                this.image = image
            }

            val doneSeverity = uiLogic.getDoneSeverity()
            dismissShouldRetry =
                doneSeverity == MessageSeverity.ERROR && uiLogic.canReloadFromDapp()
            val imageToShow =
                if (uiLogic.txId != null && doneSeverity == MessageSeverity.INFORMATION)
                    IMAGE_TX_DONE
                else
                    doneSeverity.getImage()

            image?.isHidden = imageToShow == null
            imageToShow?.let {
                image?.image = getIosSystemImage(it, UIImageSymbolScale.Large)
            }
            descLabel?.text = uiLogic.getDoneMessage(IosStringProvider(texts))
            dismissButton?.setTitle(
                texts.get(if (dismissShouldRetry) STRING_BUTTON_RETRY else STRING_BUTTON_DONE),
                UIControlState.Normal
            )
            val showRatingPrompt = uiLogic.showRatingPrompt()
            rateLabel?.isHidden = !showRatingPrompt
            rateButton?.isHidden = !showRatingPrompt
        }
    }

    inner class TransactionWithHeaderContainer :
        SigningTransactionContainer(texts, this, { startPayment() }) {
        private val messageFromDApp = Body1Label()
        private val messageIcon = UIImageView().apply {
            tintColor = UIColor.secondaryLabel()
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedWidth(40.0)
        }
        private val cardView = CardView()

        init {
            insertArrangedSubview(cardView, 0)

            cardView.contentView.addSubview(messageFromDApp)
            cardView.contentView.addSubview(messageIcon)

            val messageStackView = UIStackView(NSArray(messageIcon, messageFromDApp)).apply {
                axis = UILayoutConstraintAxis.Horizontal
                spacing = DEFAULT_MARGIN * 2
            }
            cardView.contentView.addSubview(messageStackView)
            messageStackView.edgesToSuperview(inset = DEFAULT_MARGIN)
        }

        fun showTransactionInfo() {
            bindTransaction(uiLogic.transactionInfo!!,
                tokenClickListener = { tokenId ->
                    presentViewController(TokenInformationViewController(tokenId, null), true) {}
                },
                addressLabelHandler = { address, callback ->
                    viewControllerScope.launch {
                        val appDelegate = getAppDelegate()
                        getAddressLabelFromDatabase(
                            appDelegate.database, address,
                            IosStringProvider(appDelegate.texts)
                        )?.let { runOnMainThread { callback(it) } }
                    }
                })

            cardView.isHidden = uiLogic.epsr?.message?.let {
                messageFromDApp.text = texts.format(STRING_LABEL_MESSAGE_FROM_DAPP, it)


                messageIcon.isHidden = uiLogic.epsr!!.messageSeverity.getImage()?.let {
                    messageIcon.image = getIosSystemImage(it, UIImageSymbolScale.Medium)
                    false
                } ?: true
                false
            } ?: true
        }
    }

    class FetchDataContainer(private val texts: StringProvider) : UIView(CGRect.Zero()) {
        private val progressIndicator = UIActivityIndicatorView().apply {
            activityIndicatorViewStyle = UIActivityIndicatorViewStyle.Large
        }
        private val fetchDataLabel = Headline2Label().apply {
            text = texts.getString(STRING_LABEL_FETCHING_DATA)
            textAlignment = NSTextAlignment.Center
        }

        init {
            layoutMargins = UIEdgeInsets.Zero()
            addSubview(progressIndicator)
            addSubview(fetchDataLabel)
            progressIndicator.centerHorizontal().topToSuperview()
            fetchDataLabel.widthMatchesSuperview()
                .topToBottomOf(progressIndicator, DEFAULT_MARGIN * 2)
                .bottomToSuperview()
            progressIndicator.startAnimating()
        }

    }

    inner class IosErgoPaySigningUiLogic : ErgoPaySigningUiLogic() {
        private val progressViewController =
            ProgressViewController.ProgressViewControllerPresenter(this@ErgoPaySigningViewController)
        override val coroutineScope: CoroutineScope
            get() = viewControllerScope


        override fun notifyStateChanged(newState: State) {
            runOnMainThread {
                refreshUserInterface(newState)

                if (newState == State.DONE && txId != null) {
                    doOnComplete?.invoke()
                }
            }
        }

        override fun notifyWalletStateLoaded() {
            // nothing to do, we set the label in notifyDerivedAddressChanged callback
        }

        override fun notifyDerivedAddressChanged() {
            runOnMainThread {
                val walletLabel = wallet?.walletConfig?.displayName ?: ""
                val addressLabel = derivedAddress?.getAddressLabel(IosStringProvider(texts))
                    ?: texts.format(
                        STRING_LABEL_ALL_ADDRESSES,
                        wallet?.getNumOfAddresses()
                    )
                walletAddressLabel.text =
                    texts.format(STRING_LABEL_SIGN_WITH, addressLabel, walletLabel)
            }
        }

        override fun notifyUiLocked(locked: Boolean) {
            runOnMainThread {
                progressViewController.setUiLocked(locked)
            }
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            if (!txResult.success) {
                runOnMainThread {
                    showTxResultError(txResult)
                }
            }
        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {
            runOnMainThread {
                showSigningPromptVc(signingPrompt)
            }

        }

    }

}