package org.ergoplatform.ios.transactions

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.MessageSeverity
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.ErgoPaySigningUiLogic
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getNumOfAddresses
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ErgoPaySigningViewController(
    private val request: String,
    private val walletId: Int,
    private val derivationIndex: Int = -1
) : SubmitTransactionViewController(walletId) {

    override val uiLogic = IosErgoPaySigningUiLogic()

    private val addressChooserContainer = UIView()
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
        scrollingContainer.addSubview(addressChooserContainer)
        scrollingContainer.addSubview(fetchingContainer)
        scrollingContainer.addSubview(transactionContainer)
        scrollingContainer.addSubview(stateDoneContainer)

        addressChooserContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        fetchingContainer.widthMatchesSuperview(maxWidth = MAX_WIDTH).centerVertical()
        transactionContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        stateDoneContainer.topToSuperview().bottomToSuperview(canBeLess = true)
            .widthMatchesSuperview(maxWidth = MAX_WIDTH)
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
                // nothing to do
            }
            ErgoPaySigningUiLogic.State.FETCH_DATA -> {
                // nothing to do
            }
            ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION -> transactionContainer.showTransactionInfo()
            ErgoPaySigningUiLogic.State.DONE -> showDoneInfo()
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
            // TODO Ergo Pay width is too small
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
                addArrangedSubview(dismissButton)
            }

            stateDoneContainer.contentView.addSubview(txDoneStack)
            txDoneStack.edgesToSuperview(inset = DEFAULT_MARGIN * 2)
        }
    }

    inner class TransactionWithHeaderContainer : TransactionContainer(texts, { startPayment() }) {
        private val messageFromDApp = Body1Label()
        private val messageIcon = UIImageView()
        private val cardView = CardView()

        init {
            insertArrangedSubview(cardView, 0)

            cardView.contentView.addSubview(messageFromDApp)
            cardView.contentView.addSubview(messageIcon)

            messageFromDApp.edgesToSuperview(inset = DEFAULT_MARGIN)
        }

        fun showTransactionInfo() {
            bindTransaction(uiLogic.transactionInfo!!.reduceBoxes())

            cardView.isHidden = uiLogic.epsr?.message?.let {
                messageFromDApp.text = texts.format(STRING_LABEL_MESSAGE_FROM_DAPP, it)
                // TODO Ergo Pay
                //val severityResId = getSeverityDrawableResId(
                //    uiLogic.epsr?.messageSeverity ?: MessageSeverity.NONE
                //)
                //binding.imageTiMessage.setImageResource(severityResId)
                //binding.imageTiMessage.visibility = if (severityResId == 0) View.GONE else View.VISIBLE
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