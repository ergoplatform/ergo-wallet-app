package org.ergoplatform.ios.wallet

import org.ergoplatform.ios.ui.CardView
import org.ergoplatform.ios.ui.HeadingLabel
import org.ergoplatform.ios.ui.edgesToSuperview
import org.ergoplatform.persistance.WalletConfig
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*

private const val WALLET_CELL = "Wallet"

class WalletViewController : UIViewController() {

    private val tableView = UITableView(CGRect.Zero())


    override fun viewDidLoad() {
        view.backgroundColor = UIColor.white()
        navigationItem.rightBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Add)

        view.addSubview(tableView)
        tableView.edgesToSuperview(true)

        val element = WalletConfig(0, "Test", "9sxkls", 0, null, true)
        val walletsAdapter =
            WalletsAdapter(listOf(element, element))
        tableView.setDelegate(walletsAdapter)
        tableView.dataSource = walletsAdapter
        tableView.separatorStyle = UITableViewCellSeparatorStyle.None
        tableView.refreshControl = UIRefreshControl()
        tableView.registerReusableCellClass(WalletCell::class.java, WALLET_CELL)
    }

    override fun viewWillAppear(p0: Boolean) {
        super.viewWillAppear(p0)
        // TODO register observers
        tableView.reloadData()
        // TODO tableview.refreshControl.beginRefreshing()
    }

    override fun viewWillDisappear(p0: Boolean) {
        super.viewWillDisappear(p0)
        // TODO unregister observers
        tableView.refreshControl.endRefreshing()
    }

    class WalletsAdapter(var data: List<WalletConfig>) : UITableViewDelegateAdapter(),
        UITableViewDataSource {
        override fun getHeightForRow(tableView: UITableView?, indexPath: NSIndexPath?): Double {
            // TODO different when empty
            return 200.0
        }

        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            // TODO always 1 (for empty)
            return data.size.toLong()
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            val cell = p0.dequeueReusableCell(WALLET_CELL)
            (cell as? WalletCell)?.bind(data.get(p1.row))
            return cell
        }

        override fun getNumberOfSections(p0: UITableView?): Long {
            return 1
        }

        override fun getTitleForHeader(p0: UITableView?, p1: Long): String? {
            return null
        }

        override fun getTitleForFooter(p0: UITableView?, p1: Long): String? {
            return null
        }

        override fun canEditRow(p0: UITableView?, p1: NSIndexPath?): Boolean {
            return false
        }

        override fun canMoveRow(p0: UITableView?, p1: NSIndexPath?): Boolean {
            return false
        }

        override fun getSectionIndexTitles(p0: UITableView?): MutableList<String>? {
            return null
        }

        override fun getSectionForSectionIndexTitle(p0: UITableView?, p1: String?, p2: Long): Long {
            return 0
        }

        override fun commitEditingStyleForRow(
            p0: UITableView?,
            p1: UITableViewCellEditingStyle?,
            p2: NSIndexPath?
        ) {

        }

        override fun moveRow(p0: UITableView?, p1: NSIndexPath?, p2: NSIndexPath?) {

        }

    }

    class WalletCell : UITableViewCell(UITableViewCellStyle.Default, WALLET_CELL) {
        lateinit var nameLabel: HeadingLabel

        override fun init(p0: NSCoder?): Long {
            val init = super.init(p0)
            setupView()
            return init
        }

        override fun init(p0: UITableViewCellStyle?, p1: String?): Long {
            val init = super.init(p0, p1)
            setupView()
            return init
        }

        private fun setupView() {
            nameLabel = HeadingLabel()
            this.selectionStyle = UITableViewCellSelectionStyle.None
            val cardView = CardView()
            contentView.addSubview(cardView)
            contentView.directionalLayoutMargins = NSDirectionalEdgeInsets.Zero()
            cardView.contentView.addSubview(nameLabel)

            // safe area is bigger than layout here due to margins
            cardView.edgesToSuperview(true, 0.0, 6.0, 6.0, 0.0, 400.0)
            nameLabel.edgesToSuperview(false, 1.0, 1.0, 1.0, 1.0)
            //nameLabel.textAlignment = NSTextAlignment.Center

        }

        fun bind(walletData: WalletConfig) {
            nameLabel.text = walletData.displayName
        }

    }
}