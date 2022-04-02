package org.ergoplatform.ios.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.getExplorerAddressUrl
import org.ergoplatform.ios.tokens.TokenInformationViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.transactions.TransactionListManager
import org.ergoplatform.uilogic.STRING_TRANSACTIONS_LOAD_ALL_BUTTON
import org.ergoplatform.uilogic.STRING_TRANSACTIONS_LOAD_ALL_DESC
import org.ergoplatform.uilogic.STRING_TRANSACTIONS_NONE_YET
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getDerivedAddressEntity
import org.robovm.apple.coregraphics.CGPoint
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*
import kotlin.math.max

private const val transactionCellId = "TRANSACTION_CELL"
private const val emptyCellId = "EMPTY_CELL"
private const val pageLimit = 50

class AddressTransactionsViewController(
    private val walletId: Int,
    private val derivationIdx: Int
) : CoroutineViewController() {

    private val texts = getAppDelegate().texts
    private val tableView = UITableView(CGRect.Zero())

    private val shownData = ArrayList<AddressTransactionWithTokens>()
    private var nextPageToLoad = 0
    private var finishedLoading = false

    private var wallet: Wallet? = null
    private var shownAddress: WalletAddress? = null

    private lateinit var header: HeaderView

    override fun viewDidLoad() {
        val shareButton = UIBarButtonItem(UIBarButtonSystemItem.Action)
        navigationItem.rightBarButtonItem = shareButton
        shareButton.tintColor = UIColor.label()
        shareButton.setOnClickListener {
            shareText(getExplorerAddressUrl(shownAddress!!.publicAddress), shareButton)
        }

        header = HeaderView()
        view.addSubview(tableView)
        view.addSubview(header)
        header.widthMatchesSuperview(true).topToSuperview()
        tableView.widthMatchesSuperview(true).bottomToSuperview(true).topToBottomOf(header)

        tableView.dataSource = TransactionsDataSource()
        tableView.separatorStyle = UITableViewCellSeparatorStyle.None
        tableView.setAllowsSelection(false)
        val uiRefreshControl = UIRefreshControl()
        tableView.refreshControl = uiRefreshControl
        uiRefreshControl.addOnValueChangedListener {
            if (uiRefreshControl.isRefreshing) {
                uiRefreshControl.endRefreshing()
                refreshListWhenAtTop()
                refreshAddress()
            }
        }
        tableView.registerReusableCellClass(AddressTransactionCell::class.java, transactionCellId)
        tableView.registerReusableCellClass(EmptyCell::class.java, emptyCellId)
        tableView.rowHeight = UITableView.getAutomaticDimension()
        tableView.estimatedRowHeight = UITableView.getAutomaticDimension()
    }

    private fun refreshAddress() {
        val appDelegate = getAppDelegate()
        TransactionListManager.downloadTransactionListForAddress(
            shownAddress!!.publicAddress,
            ErgoApiService.getOrInit(appDelegate.prefs),
            appDelegate.database
        )
    }

    override fun viewWillAppear(p0: Boolean) {
        super.viewWillAppear(p0)
        val appDelegate = getAppDelegate()
        if (wallet == null) {
            viewControllerScope.launch(Dispatchers.IO) {
                appDelegate.database.walletDbProvider.loadWalletWithStateById(walletId)?.let { wallet ->
                    this@AddressTransactionsViewController.wallet = wallet
                    shownAddress = wallet.getDerivedAddressEntity(derivationIdx)
                    onResume()
                    runOnMainThread { newAddressChosen() }
                }
            }
        } else {
            onResume()
        }

        viewControllerScope.launch {
            TransactionListManager.isDownloading.collect { refreshing ->
                runOnMainThread {
                    header.isRefreshing = refreshing
                    if (!refreshing) {
                        runOnMainThread {
                            // refresh view, but only when at top of the list
                            refreshListWhenAtTop()
                        }
                    }
                }
            }
        }
        viewControllerScope.launch {
            TransactionListManager.downloadProgress.collect {
                if (TransactionListManager.downloadAddress.value == shownAddress?.publicAddress) {
                    finishedLoading = false
                    refreshListWhenAtTop()
                }
            }
        }
    }

    private fun refreshListWhenAtTop() {
        if (tableView.contentOffset.y <= 0.0 || nextPageToLoad == 0 && !finishedLoading)
            refreshListShownData()
    }

    private fun newAddressChosen() {
        header.addressLabel.text = shownAddress?.getAddressLabel(IosStringProvider(texts))
        refreshListShownData()
    }

    private fun refreshListShownData() {
        LogUtils.logDebug(this.javaClass.simpleName, "Refreshing shown list completely")
        // complete refresh
        resetShownData()
        fetchNextChunkFromDb()
    }

    private fun resetShownData() {
        nextPageToLoad = 0
        finishedLoading = false
        shownData.clear()
    }

    private fun fetchNextChunkFromDb() {
        if (finishedLoading)
            return

        val pageToLoad = nextPageToLoad
        shownAddress?.let { address ->
            viewControllerScope.launch {
                val txLoaded = getAppDelegate().database.transactionDbProvider.loadAddressTransactionsWithTokens(
                    address.publicAddress,
                    pageLimit, pageToLoad
                )
                runOnMainThread {
                    if (txLoaded.isNotEmpty()) {
                        // search if first element is already in shown list
                        val idxInList =
                            shownData.indexOfFirst { it.addressTransaction.txId == txLoaded.first().addressTransaction.txId }
                        // if it is, remove the last elements so that we don't have them in list multiple times
                        if (idxInList >= 0) while (shownData.size > idxInList)
                            shownData.removeLast()
                    }

                    shownData.addAll(txLoaded)

                    // if we reload from the beginning, set table view to the top position
                    if (pageToLoad == 0) {
                        // yes, this is needed
                        // https://stackoverflow.com/a/50606137/7487013
                        tableView.setContentOffset(CGPoint.Zero(), false)
                    }
                    tableView.reloadData()
                    if (pageToLoad == 0) {
                        tableView.layoutIfNeeded()
                        tableView.setContentOffset(CGPoint.Zero(), false)
                    }
                }

                finishedLoading = txLoaded.size < pageLimit
                if (!finishedLoading)
                    nextPageToLoad = pageToLoad + 1
            }
        }
    }

    override fun onResume() {
        refreshAddress()
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        tableView.refreshControl.endRefreshing()
    }

    inner class TransactionsDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            // When we already have data to show, show the empty cell if no wallets configured
            return max(1, shownData.size.toLong())
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            if (shownData.isEmpty()) {
                return p0.dequeueReusableCell(emptyCellId)
            } else {
                val cell = p0.dequeueReusableCell(transactionCellId)
                val itemIndex = p1.row
                val isLastItem = itemIndex == shownData.size - 1
                (cell as? AddressTransactionCell)?.bind(
                    shownData[itemIndex], this@AddressTransactionsViewController,
                    isLastItem && finishedLoading
                )
                if (isLastItem) {
                    fetchNextChunkFromDb()
                }
                return cell
            }
        }

        override fun getNumberOfSections(p0: UITableView?): Long {
            return 1
        }

        override fun canEditRow(p0: UITableView?, p1: NSIndexPath?): Boolean {
            return false
        }

        override fun canMoveRow(p0: UITableView?, p1: NSIndexPath?): Boolean {
            return false
        }
    }

    inner class HeaderView : UIView(CGRect.Zero()) {
        private val refreshView = UIActivityIndicatorView(UIActivityIndicatorViewStyle.Medium)
        private val addressSelector =
            buildAddressSelectorView(
                this@AddressTransactionsViewController, walletId,
                showAllAddresses = false,
                keepWidth = true
            ) {
                shownAddress = wallet?.getDerivedAddressEntity(it!!)
                newAddressChosen()
            }
        val addressLabel get() = addressSelector.content

        var isRefreshing: Boolean = false
            set(refreshing) {
                field = refreshing
                if (refreshing) {
                    refreshView.startAnimating()
                } else {
                    refreshView.stopAnimating()
                }
            }

        init {
            addSubview(refreshView)
            addSubview(addressSelector)
            refreshView.rightToSuperview().topToSuperview().bottomToSuperview().fixedWidth(30.0)
            addressSelector.centerVertical().centerHorizontal(true)
            backgroundColor = UIColor.secondarySystemBackground()
        }

    }

    class EmptyCell : AbstractTableViewCell(emptyCellId) {
        override fun setupView() {
            val label = Body1Label().apply {
                text = getAppDelegate().texts.get(STRING_TRANSACTIONS_NONE_YET)
                textAlignment = NSTextAlignment.Center
            }
            contentView.addSubview(label)
            label.centerHorizontal(true).topToSuperview(topInset = DEFAULT_MARGIN * 10).bottomToSuperview()
        }

    }

    class AddressTransactionCell : AbstractTableViewCell(transactionCellId) {
        private lateinit var txView: AddressTransactionEntryView
        private lateinit var lastItemLabel: UIView
        private lateinit var lastItemButton: TextButton
        private var vc: AddressTransactionsViewController? = null
        private var tx: AddressTransactionWithTokens? = null

        override fun setupView() {
            val cardView = CardView()
            txView = AddressTransactionEntryView()

            val texts = getAppDelegate().texts

            val innerLabel = Body1Label().apply {
                textAlignment = NSTextAlignment.Center
                text = texts.get(STRING_TRANSACTIONS_LOAD_ALL_DESC)
            }
            lastItemLabel = UIView(CGRect.Zero()).apply {
                addSubview(innerLabel)
                innerLabel.edgesToSuperview(inset = DEFAULT_MARGIN)
            }

            lastItemButton = TextButton(texts.get(STRING_TRANSACTIONS_LOAD_ALL_BUTTON))
            lastItemButton.addOnTouchUpInsideListener { _, _ ->
                val appDelegate = getAppDelegate()
                if (TransactionListManager.startDownloadAllAddressTransactions(
                        vc!!.shownAddress!!.publicAddress,
                        ErgoApiService.getOrInit(appDelegate.prefs), appDelegate.database
                    )
                ) {
                    vc!!.resetShownData()
                }
            }

            val stackView = UIStackView(NSArray(cardView, lastItemLabel, lastItemButton)).apply {
                axis = UILayoutConstraintAxis.Vertical
            }

            contentView.addSubview(stackView)

            stackView.widthMatchesSuperview(true, DEFAULT_MARGIN, MAX_WIDTH)
                .superViewWrapsHeight(true, 0.0)

            cardView.contentView.addSubview(txView)
            cardView.contentView.layoutMargins = UIEdgeInsets.Zero()
            txView.apply {
                edgesToSuperview(inset = DEFAULT_MARGIN)
                isUserInteractionEnabled = true
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    vc!!.navigationController.pushViewController(
                        TransactionInfoViewController(tx!!.addressTransaction.txId), true
                    )
                })

            }
        }

        fun bind(tx: AddressTransactionWithTokens, vc: AddressTransactionsViewController, isLastElement: Boolean) {
            this.vc = vc
            this.tx = tx
            txView.bind(tx, tokenClickListener = { tokenId ->
                vc.presentViewController(TokenInformationViewController(tokenId, null), true) {}
            }, getAppDelegate().texts)
            lastItemButton.isHidden = !isLastElement
            lastItemLabel.isHidden = !isLastElement
        }

    }
}