package org.ergoplatform.ios.transactions

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.ios.ui.*
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
    private val walletId: Int,
    private val derivationIndex: Int = -1
) : SubmitTransactionViewController(walletId) {

    override val uiLogic = IosErgoPaySigningUiLogic()

    private val addressChooserContainer = CardView()
    private lateinit var fetchingContainer: FetchDataContainer
    private lateinit var transactionContainer: TransactionWithHeaderContainer
    private val stateDoneContainer = CardView()
    private val walletAddressLabel = Body2BoldLabel().apply {
        numberOfLines = 1
        textColor = uiColorErgo
        textAlignment = NSTextAlignment.Center
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        val appDelegate = getAppDelegate()
        texts = appDelegate.texts
        fetchingContainer = FetchDataContainer()
        transactionContainer = TransactionWithHeaderContainer()

        title = texts.get(STRING_TITLE_ERGO_PAY_REQUEST)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        view.layoutMargins = UIEdgeInsets.Zero()
        view.addSubview(walletAddressLabel)


        walletAddressLabel.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview(inset = DEFAULT_MARGIN)

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
        scrollView.topToBottomOf(walletAddressLabel, DEFAULT_MARGIN).widthMatchesSuperview().bottomToSuperview()

        uiLogic.init(
            request,
            walletId,
            derivationIndex,
            appDelegate.database,
            appDelegate.prefs,
            IosStringProvider(texts)
        )
    }

    override fun onAddressChosen(it: Int?) {
        super.onAddressChosen(it)
        // redo the request - can't be done within uilogic because context is needed on Android
        uiLogic.lastRequest?.let {
            val appDelegate = getAppDelegate()
            uiLogic.hasNewRequest(
                it,
                appDelegate.prefs,
                IosStringProvider(appDelegate.texts)
            )
        }

    }

    private fun refreshUserInterface(state: ErgoPaySigningUiLogic.State) {
        addressChooserContainer.isHidden = state != ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS
        fetchingContainer.isHidden = state != ErgoPaySigningUiLogic.State.FETCH_DATA
        transactionContainer.isHidden = state != ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION
        stateDoneContainer.isHidden = state != ErgoPaySigningUiLogic.State.DONE

        when (state) {
            ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS -> {
                populateWaitForAddressView()
            }
            ErgoPaySigningUiLogic.State.FETCH_DATA -> {
                // nothing to do
            }
            ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> transactionContainer.showTransactionInfo()
            ErgoPaySigningUiLogic.State.DONE -> showDoneInfo()
        }
    }

    private fun populateWaitForAddressView() {
        if (addressChooserContainer.contentView.subviews.isEmpty()) {
            val image = UIImageView(ergoLogoImage.imageWithTintColor(UIColor.label())).apply {
                fixedHeight(100.0)
                contentMode = UIViewContentMode.ScaleAspectFit
            }

            val label = Body1Label().apply {
                text = texts.get(STRING_LABEL_ERGO_PAY_CHOOSE_ADDRESS)
                textAlignment = NSTextAlignment.Center
            }

            val button = PrimaryButton(texts.get(STRING_TITLE_CHOOSE_ADDRESS)).apply {
                addOnTouchUpInsideListener { _, _ -> showChooseAddressList(false) }
            }

            addressChooserContainer.contentView.apply {
                addSubview(image)
                addSubview(label)
                addSubview(button)

                image.topToSuperview(topInset = DEFAULT_MARGIN * 2).centerHorizontal()
                label.topToBottomOf(image, DEFAULT_MARGIN * 3).widthMatchesSuperview(inset = DEFAULT_MARGIN)
                button.topToBottomOf(label, DEFAULT_MARGIN * 2).centerHorizontal()
                    .bottomToSuperview(bottomInset = DEFAULT_MARGIN * 2)
            }
        }
    }

    private fun getImageFromSeverity(severity: MessageSeverity): String? {
        return when (severity) {
            MessageSeverity.NONE -> null
            MessageSeverity.INFORMATION -> IMAGE_INFORMATION
            MessageSeverity.WARNING -> IMAGE_WARNING
            MessageSeverity.ERROR -> IMAGE_ERROR
        }
    }

    private fun showDoneInfo() {
        if (stateDoneContainer.contentView.subviews.isEmpty()) {
            val image = getImageFromSeverity(uiLogic.getDoneSeverity())?.let {
                UIImageView(getIosSystemImage(it, UIImageSymbolScale.Large)).apply {
                    contentMode = UIViewContentMode.ScaleAspectFit
                    tintColor = uiColorErgo
                    fixedHeight(100.0)
                }
            }

            val descLabel = Body1Label()
            descLabel.text = uiLogic.getDoneMessage(IosStringProvider(texts))
            descLabel.textAlignment = NSTextAlignment.Center

            val dismissButton = PrimaryButton(texts.get(STRING_LABEL_DISMISS))
            dismissButton.addOnTouchUpInsideListener { _, _ -> navigationController.popViewController(true) }
            val doneButtonContainer = UIView()
            doneButtonContainer.addSubview(dismissButton)
            dismissButton.centerHorizontal().topToSuperview().bottomToSuperview().fixedWidth(150.0)

            val txDoneStack = UIStackView().apply {
                axis = UILayoutConstraintAxis.Vertical
                spacing = DEFAULT_MARGIN * 3

                image?.let { addArrangedSubview(image) }
                addArrangedSubview(descLabel)
                addArrangedSubview(doneButtonContainer)
            }

            stateDoneContainer.contentView.addSubview(txDoneStack)
            txDoneStack.edgesToSuperview(inset = DEFAULT_MARGIN * 2)
        }
    }

    inner class TransactionWithHeaderContainer : TransactionContainer(texts, { startPayment() }) {
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
            bindTransaction(uiLogic.transactionInfo!!.reduceBoxes())

            cardView.isHidden = uiLogic.epsr?.message?.let {
                messageFromDApp.text = texts.format(STRING_LABEL_MESSAGE_FROM_DAPP, it)


                messageIcon.isHidden = getImageFromSeverity(uiLogic.epsr!!.messageSeverity)?.let {
                    messageIcon.image = getIosSystemImage(it, UIImageSymbolScale.Medium)
                    false
                } ?: true
                false
            } ?: true
        }
    }

    inner class FetchDataContainer : UIView(CGRect.Zero()) {
        private val progressIndicator = UIActivityIndicatorView().apply {
            activityIndicatorViewStyle = UIActivityIndicatorViewStyle.Large
        }
        private val fetchDataLabel = Headline2Label().apply {
            text = texts.get(STRING_LABEL_FETCHING_DATA)
            textAlignment = NSTextAlignment.Center
        }

        init {
            layoutMargins = UIEdgeInsets.Zero()
            addSubview(progressIndicator)
            addSubview(fetchDataLabel)
            progressIndicator.centerHorizontal().topToSuperview()
            fetchDataLabel.widthMatchesSuperview().topToBottomOf(progressIndicator, DEFAULT_MARGIN * 2)
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
                walletAddressLabel.text = texts.format(STRING_LABEL_SIGN_WITH, addressLabel, walletLabel)
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