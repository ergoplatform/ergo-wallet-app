package org.ergoplatform.ios.wallet

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.ios.BottomNavigationBar
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_LABEL_ERG_PRICE
import org.ergoplatform.uilogic.STRING_LABEL_LAST_SYNC
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.utils.getTimeSpanString
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*
import org.robovm.objc.annotation.CustomClass
import kotlin.math.max

class WalletViewController : CoroutineViewController() {

    private val tableView = UITableView(CGRect.Zero())

    // the data currently to be shown, only change on main thread
    private val shownData = ArrayList<Wallet>()
    private var loadedDataChanged = false

    // the data that should get shown on next table refresh
    private val loadedData = ArrayList<Wallet>()

    // did we actually already fetch dada from the db at least once?
    private var hasDataToShow = false

    private lateinit var header: HeaderView

    override fun viewDidLoad() {
        val addWalletButton = UIBarButtonItem(UIBarButtonSystemItem.Add)
        navigationItem.rightBarButtonItem = addWalletButton
        addWalletButton.tintColor = UIColor.label()
        addWalletButton.setOnClickListener {
            val navController = UINavigationController(AddWalletChooserViewController())
            navController.modalPresentationStyle = UIModalPresentationStyle.FormSheet
            navController.isModalInPresentation = true
            this.presentViewController(navController, true) { }
        }

        val uiBarButtonItem = UIBarButtonItem(
            getIosSystemImage(IMAGE_QR_SCAN, UIImageSymbolScale.Small),
            UIBarButtonItemStyle.Plain
        )
        uiBarButtonItem.setOnClickListener {
            presentViewController(QrScannerViewController(dismissAnimated = false) {
                (navigationController.parentViewController as? BottomNavigationBar)?.handlePaymentRequest(it, true)
            }, true) {}
        }
        uiBarButtonItem.tintColor = UIColor.label()
        navigationItem.leftBarButtonItem = uiBarButtonItem

        view.addSubview(tableView)
        tableView.edgesToSuperview(true)

        tableView.dataSource = WalletDataSource()
        tableView.separatorStyle = UITableViewCellSeparatorStyle.None
        val uiRefreshControl = UIRefreshControl()
        tableView.refreshControl = uiRefreshControl
        uiRefreshControl.addOnValueChangedListener {
            if (uiRefreshControl.isRefreshing) {
                uiRefreshControl.endRefreshing()
                val appDelegate = getAppDelegate()
                NodeConnector.getInstance().refreshByUser(appDelegate.prefs, appDelegate.database)
            }
        }
        tableView.registerReusableCellClass(WalletCell::class.java, WALLET_CELL)
        tableView.registerReusableCellClass(EmptyCell::class.java, EMPTY_CELL)
        tableView.rowHeight = UITableView.getAutomaticDimension()
        tableView.estimatedRowHeight = UITableView.getAutomaticDimension()

        header = HeaderView()
        tableView.tableHeaderView = header
        tableView.tableHeaderView.backgroundColor = UIColor.secondarySystemBackground()
    }

    override fun viewWillAppear(p0: Boolean) {
        super.viewWillAppear(p0)
        val appDelegate = getAppDelegate()
        val nodeConnector = NodeConnector.getInstance()
        viewControllerScope.launch {
            appDelegate.database.getWalletsWithStatesFlow().collect {
                LogUtils.logDebug("WalletViewController", "New wallet data from flow change")
                newDataLoaded(it)
                // do we have a new wallet or wallet address and need to trigger NodeConnector?
                val needStateRefresh = (NodeConnector.getInstance().lastRefreshMs == 0L)
                runOnMainThread {
                    refreshListShownData()
                    if (needStateRefresh) {
                        onResume()
                    }
                }
            }
        }
        viewControllerScope.launch {
            nodeConnector.isRefreshing.collect { refreshing ->
                runOnMainThread {
                    header.isRefreshing = refreshing
                }
                if (!refreshing) {
                    LogUtils.logDebug(
                        "WalletViewController",
                        "Node connector refresh done, reload shown data"
                    )
                    newDataLoaded(appDelegate.database.getWalletsWithStates())
                    runOnMainThread { refreshListShownData() }
                }
            }
        }
        viewControllerScope.launch {
            nodeConnector.fiatValue.collect {
                runOnMainThread {
                    header.refreshFiatValue()
                }
            }
        }

        onResume()
    }

    private fun newDataLoaded(walletData: List<Wallet>) {
        synchronized(loadedData) {
            loadedData.clear()
            loadedData.addAll(walletData)
            loadedDataChanged = true
        }
    }

    private fun refreshListShownData() {
        LogUtils.logDebug(
            "WalletViewController",
            "Refresh changed wallet data in UI: $loadedDataChanged"
        )
        if (loadedDataChanged) {
            synchronized(loadedData) {
                loadedDataChanged = false
                shownData.clear()
                shownData.addAll(loadedData.sortedBy { it.walletConfig.displayName })
            }
            hasDataToShow = true
            tableView.reloadData()
        }
    }

    override fun onResume() {
        header.updateLastRefreshLabel()
        val appDelegate = getAppDelegate()
        NodeConnector.getInstance().refreshWhenNeeded(
            appDelegate.prefs,
            appDelegate.database
        )
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        tableView.refreshControl.endRefreshing()
    }

    inner class WalletDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            // When we already have data to show, show the empty cell if no wallets configured
            return max(if (hasDataToShow) 1 else 0, shownData.size.toLong())
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            if (shownData.isEmpty()) {
                val cell = p0.dequeueReusableCell(EMPTY_CELL)
                (cell as? EmptyCell)?.walletChooserStackView?.clickListener = { vc ->
                    val navController = UINavigationController(vc)
                    navController.modalPresentationStyle = UIModalPresentationStyle.FormSheet
                    navController.isModalInPresentation = true
                    presentViewController(navController, true) { }
                }
                return cell
            } else {
                val cell = p0.dequeueReusableCell(WALLET_CELL)
                (cell as? WalletCell)?.let {
                    it.bind(shownData.get(p1.row))
                    it.clickListener =
                        { vc -> this@WalletViewController.navigationController.pushViewController(vc, true) }
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

    @CustomClass
    class HeaderView : UIView(CGRect(0.0, 0.0, 0.0, 70.0)) {
        private val ergoLogo: UIImageView
        private val noConnection: UIImageView
        private val fiatLabel = Body1Label()
        private val syncLabel = Body1Label()
        private val refreshView = UIActivityIndicatorView()
        private val stringProvider = IosStringProvider(getAppDelegate().texts)

        var isRefreshing: Boolean = false
            set(refreshing) {
                field = refreshing
                if (refreshing) {
                    refreshView.startAnimating()
                    noConnection.isHidden = true
                } else {
                    refreshView.stopAnimating()
                    updateLastRefreshLabel()
                }
            }

        init {
            ergoLogo = UIImageView(ergoLogoImage.imageWithTintColor(UIColor.label()))
            ergoLogo.contentMode = UIViewContentMode.ScaleAspectFit

            noConnection = UIImageView(
                getIosSystemImage(IMAGE_NO_CONNECTION, UIImageSymbolScale.Small)
            )
            noConnection.contentMode = UIViewContentMode.Center
            noConnection.tintColor = UIColor.systemRed()
            noConnection.isHidden = true

            val stackview = UIStackView(NSArray(fiatLabel, syncLabel))
            stackview.axis = UILayoutConstraintAxis.Vertical
            addSubview(stackview)
            addSubview(ergoLogo)
            addSubview(refreshView)
            addSubview(noConnection)
            ergoLogo.leftToSuperview().topToSuperview().bottomToSuperview().fixedWidth(frame.height)
            noConnection.rightToSuperview().topToSuperview().bottomToSuperview().fixedWidth(frame.height)
            refreshView.rightToSuperview().topToSuperview().bottomToSuperview().fixedWidth(frame.height)
            stackview.topToSuperview().bottomToSuperview().leftToRightOf(ergoLogo).rightToLeftOf(refreshView)
        }

        fun updateLastRefreshLabel() {
            val nodeConnector = NodeConnector.getInstance()
            val lastRefreshMs = nodeConnector.lastRefreshMs
            val lastRefreshTimeSpan = (System.currentTimeMillis() - lastRefreshMs) / 1000L
            val timeSpanString: String = getTimeSpanString(lastRefreshTimeSpan, stringProvider)
            syncLabel.text =
                if (lastRefreshMs == 0L) " " // needs to be a blank, iOS handles empty like hidden
                else stringProvider.getString(STRING_LABEL_LAST_SYNC, timeSpanString)
            noConnection.isHidden = !nodeConnector.lastHadError
        }

        fun refreshFiatValue() {
            val nodeConnector = NodeConnector.getInstance()
            val ergoPrice = nodeConnector.fiatValue.value
            fiatLabel.text =
                if (ergoPrice == 0f) " " // needs to be a blank, iOS handles empty like hidden
                else stringProvider.getString(STRING_LABEL_ERG_PRICE) + " " +
                        formatFiatToString(ergoPrice.toDouble(), nodeConnector.fiatCurrency, stringProvider)
        }
    }

}