package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.ios.tokens.TokenInformationViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.TransactionInfoUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class TransactionInfoViewController(
    private val txId: String,
    private val address: String?,
) : CoroutineViewController() {
    private val uiLogic = IosTransactionInfoUiLogic()
    private lateinit var infoContainer: TransactionInfoContainer
    private lateinit var activityView: UIActivityIndicatorView
    private lateinit var errorView: UIView

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val texts = getAppDelegate().texts
        title = texts.get(STRING_TITLE_TRANSACTION)

        val shareButton = UIBarButtonItem(UIBarButtonSystemItem.Action)
        navigationItem.rightBarButtonItem = shareButton
        shareButton.tintColor = UIColor.label()
        shareButton.setOnClickListener {
            shareText(getExplorerTxUrl(txId), shareButton)
        }

        infoContainer = TransactionInfoContainer(texts)
        infoContainer.layoutMargins =
            UIEdgeInsets(DEFAULT_MARGIN * 2, DEFAULT_MARGIN * 2, DEFAULT_MARGIN * 2, DEFAULT_MARGIN * 2)

        activityView = UIActivityIndicatorView(UIActivityIndicatorViewStyle.Large)
        view.addSubview(activityView)
        activityView.centerVertical().centerHorizontal()

        errorView = UIImageView(getIosSystemImage(IMAGE_WARNING, UIImageSymbolScale.Large)).apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            tintColor = UIColor.secondaryLabel()
            fixedHeight(100.0)
        }
        view.addSubview(errorView)
        errorView.centerVertical().centerHorizontal()

        val scrollView = infoContainer.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.edgesToSuperview(true, maxWidth = MAX_WIDTH)
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        loadTx()
    }

    private fun loadTx(forceReload: Boolean = false) {
        val appDelegate = getAppDelegate()
        uiLogic.init(
            txId,
            address,
            ApiServiceManager.getOrInit(appDelegate.prefs),
            appDelegate.database,
            forceReload
        )
        if (uiLogic.isLoading)
            refreshScreenState(null)
    }

    fun refreshScreenState(ti: TransactionInfo?) {
        if (!uiLogic.isLoading) {
            errorView.isHidden = ti != null
            activityView.isHidden = true
            activityView.stopAnimating()
            infoContainer.isHidden = ti == null
        } else {
            errorView.isHidden = true
            activityView.isHidden = false
            activityView.startAnimating()
            infoContainer.isHidden = true
        }
    }

    inner class IosTransactionInfoUiLogic : TransactionInfoUiLogic() {
        override val coroutineScope get() = viewControllerScope

        override fun onTransactionInformationFetched(ti: TransactionInfo?) {
            runOnMainThread {
                refreshScreenState(ti)
                ti?.let {
                    infoContainer.bindTransaction(ti,
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
                }
            }
        }

    }

    inner class TransactionInfoContainer(private val texts: I18NBundle) :
        TransactionContainer(texts, this@TransactionInfoViewController) {
        override val titleInboxes get() = STRING_TITLE_TRANSACTION_INBOXES
        override val descInboxes get() = STRING_DESC_TRANSACTION_INBOXES
        override val titleOutboxes get() = STRING_TITLE_OUTBOXES
        override val descOutboxes get() = STRING_DESC_TRANSACTION_OUTBOXES

        private val txIdLabel = Body1BoldLabel().apply {
            textColor = uiColorErgo
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
            textAlignment = NSTextAlignment.Center
            isUserInteractionEnabled = true
            addGestureRecognizer(UILongPressGestureRecognizer {
                this@TransactionInfoViewController.shareText(text, this)
            })
        }

        private val purposeLabel = Body2Label().apply {
            numberOfLines = 3
            textAlignment = NSTextAlignment.Center
        }

        private val txTimestampLabel = Body2BoldLabel().apply {
            textAlignment = NSTextAlignment.Center
        }

        private val refreshButtonContainer = UIView(CGRect.Zero())

        init {
            val introContainer = UIView(CGRect.Zero())

            introContainer.apply {
                addSubview(purposeLabel)
                addSubview(txIdLabel)
                addSubview(txTimestampLabel)
                layoutMargins = UIEdgeInsets.Zero()

                txIdLabel.topToSuperview().widthMatchesSuperview()
                purposeLabel.topToBottomOf(txIdLabel, inset = DEFAULT_MARGIN / 2).widthMatchesSuperview()
                txTimestampLabel.topToBottomOf(purposeLabel, inset = DEFAULT_MARGIN).widthMatchesSuperview()
                    .bottomToSuperview()
            }

            val reloadButton = PrimaryButton(texts.get(STRING_BUTTON_RELOAD))
            refreshButtonContainer.addSubview(reloadButton)
            reloadButton.centerHorizontal(true).topToSuperview().bottomToSuperview()
            reloadButton.addOnTouchUpInsideListener { _, _ ->
                loadTx(forceReload = true)
            }

            insertArrangedSubview(introContainer, 0)
            insertArrangedSubview(refreshButtonContainer, 1)
        }

        override fun bindTransaction(
            transactionInfo: TransactionInfo,
            tokenClickListener: ((String) -> Unit)?,
            addressLabelHandler: ((String, (String) -> Unit) -> Unit)?,
            tokenLabelHandler: ((String, (String) -> Unit) -> Unit)?,
        ) {
            super.bindTransaction(transactionInfo, tokenClickListener, addressLabelHandler, tokenLabelHandler)

            txIdLabel.text = transactionInfo.id
            purposeLabel.text = uiLogic.transactionPurpose
            txTimestampLabel.text = uiLogic.getTransactionExecutionState(IosStringProvider(texts))
            refreshButtonContainer.isHidden = !uiLogic.shouldOfferReloadButton()
        }
    }
}