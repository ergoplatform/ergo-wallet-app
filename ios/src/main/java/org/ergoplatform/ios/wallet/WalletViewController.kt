package org.ergoplatform.ios.wallet

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_LABEL_ERG_PRICE
import org.ergoplatform.uilogic.STRING_LABEL_LAST_SYNC
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getTimeSpanString
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*
import org.robovm.objc.annotation.CustomClass
import kotlin.math.max

class WalletViewController : CoroutineViewController() {

    private val tableView = UITableView(CGRect.Zero())
    private val shownData = ArrayList<Wallet>()
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
                LogUtils.logDebug("WalletViewController", "Refresh shown wallet data from flow change")
                // do we have a new wallet or wallet address and need to trigger NodeConnector?
                val needStateRefresh = (NodeConnector.getInstance().lastRefreshMs == 0L)
                runOnMainThread {
                    refreshListShownData(it)
                    if (needStateRefresh) {
                        onResume()
                    }
                }
            }
        }
        viewControllerScope.launch {
            nodeConnector.isRefreshing.collect { refreshing ->
                runOnMainThread {
                    if (refreshing)
                        header.refreshView.startAnimating()
                    else {
                        // TODO show error state
                        header.refreshView.stopAnimating()
                        header.updateLastRefreshLabel()
                    }
                }
                if (!refreshing) {
                    LogUtils.logDebug("WalletViewController", "Refresh done, reload shown data")
                    val newData = appDelegate.database.getWalletsWithStates()
                    runOnMainThread { refreshListShownData(newData) }
                }
            }
        }
        viewControllerScope.launch {
            nodeConnector.fiatValue.collect {
                println("New fiat value")
                runOnMainThread {
                    header.refreshFiatValue()
                }
            }
        }

        onResume()
    }

    private fun refreshListShownData(newData: List<Wallet>) {
        shownData.clear()
        shownData.addAll(newData.sortedBy { it.walletConfig.displayName })
        tableView.reloadData()
    }

    override fun onResume() {
        header.updateLastRefreshLabel()
        val appDelegate = getAppDelegate()
        NodeConnector.getInstance().refreshWhenNeeded(
            appDelegate.prefs,
            appDelegate.database
        )
    }

    override fun viewWillDisappear(p0: Boolean) {
        super.viewWillDisappear(p0)
        tableView.refreshControl.endRefreshing()
    }

    inner class WalletDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            return max(1, shownData.size.toLong())
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
        private val fiatLabel = Body1Label()
        private val syncLabel = Body1Label()
        val refreshView = UIActivityIndicatorView()
        private val stringProvider = IosStringProvider(getAppDelegate().texts)

        init {
            ergoLogo = UIImageView(ergoLogoImage.imageWithTintColor(UIColor.label()))
            ergoLogo.contentMode = UIViewContentMode.ScaleAspectFit

            val stackview = UIStackView(NSArray(fiatLabel, syncLabel))
            stackview.axis = UILayoutConstraintAxis.Vertical
            addSubview(stackview)
            addSubview(ergoLogo)
            addSubview(refreshView)
            ergoLogo.leftToSuperview().topToSuperview().bottomToSuperview().fixedWidth(frame.height)
            refreshView.rightToSuperview().topToSuperview().bottomToSuperview().fixedWidth(frame.height)
            stackview.topToSuperview().bottomToSuperview().leftToRightOf(ergoLogo).rightToLeftOf(refreshView)
        }

        fun updateLastRefreshLabel() {
            val lastRefreshMs = NodeConnector.getInstance().lastRefreshMs
            syncLabel.isHidden = lastRefreshMs == 0L
            val lastRefreshTimeSpan = (System.currentTimeMillis() - lastRefreshMs) / 1000L
            val timeSpanString: String = getTimeSpanString(lastRefreshTimeSpan, stringProvider)
            syncLabel.text = stringProvider.getString(STRING_LABEL_LAST_SYNC, timeSpanString)
        }

        fun refreshFiatValue() {
            val nodeConnector = NodeConnector.getInstance()
            val ergoPrice = nodeConnector.fiatValue.value
            fiatLabel.isHidden = ergoPrice == 0f
            fiatLabel.text = stringProvider.getString(STRING_LABEL_ERG_PRICE) +
                    " " + ergoPrice.toString() + " " + nodeConnector.fiatCurrency.uppercase()
        }
    }

}