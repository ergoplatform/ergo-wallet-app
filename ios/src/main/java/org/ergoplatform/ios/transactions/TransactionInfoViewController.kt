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

        val scrollView = infoContainer.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.edgesToSuperview(true, maxWidth = MAX_WIDTH)

        infoContainer.isHidden = true
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        val appDelegate = getAppDelegate()
        uiLogic.init(
            txId,
            address,
            ApiServiceManager.getOrInit(appDelegate.prefs),
            appDelegate.database
        )
        activityView.startAnimating()
    }

    inner class IosTransactionInfoUiLogic : TransactionInfoUiLogic() {
        override val coroutineScope get() = viewControllerScope

        override fun onTransactionInformationFetched(ti: TransactionInfo?) {
            runOnMainThread {
                activityView.isHidden = true
                activityView.stopAnimating()
                ti?.let {
                    infoContainer.isHidden = false
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
                } ?: run {
                    val errorView = UIImageView(getIosSystemImage(IMAGE_WARNING, UIImageSymbolScale.Large)).apply {
                        contentMode = UIViewContentMode.ScaleAspectFit
                        tintColor = UIColor.secondaryLabel()
                        fixedHeight(100.0)
                    }
                    view.addSubview(errorView)
                    errorView.centerVertical().centerHorizontal()
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

            insertArrangedSubview(introContainer, 0)
        }

        override fun bindTransaction(
            transactionInfo: TransactionInfo,
            tokenClickListener: ((String) -> Unit)?,
            addressLabelHandler: ((String, (String) -> Unit) -> Unit)?
        ) {
            super.bindTransaction(transactionInfo, tokenClickListener, addressLabelHandler)

            txIdLabel.text = transactionInfo.id
            purposeLabel.text = uiLogic.transactionPurpose
            txTimestampLabel.text = uiLogic.getTransactionExecutionState(IosStringProvider(texts))
        }
    }
}