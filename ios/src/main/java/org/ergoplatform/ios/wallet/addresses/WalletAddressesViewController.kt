package org.ergoplatform.ios.wallet.addresses

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_ADDRESSES
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressesUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.UITableView
import org.robovm.apple.uikit.UITableViewCell
import org.robovm.apple.uikit.UITableViewCellSeparatorStyle
import org.robovm.apple.uikit.UITableViewDataSourceAdapter

const val ADDRESS_CELL = "address_cell"

class WalletAddressesViewController(val walletId: Int) : CoroutineViewController() {
    private val uiLogic = IosWalletAddressesUiLogic()

    private val tableView = UITableView(CGRect.Zero())

    override fun viewDidLoad() {
        super.viewDidLoad()

        title = getAppDelegate().texts.get(STRING_TITLE_WALLET_ADDRESSES)

        // TODO addresses table header with wallet display name

        view.addSubview(tableView)
        tableView.edgesToSuperview(true)

        tableView.apply {

            dataSource = AddressesDataSource()
            separatorStyle = UITableViewCellSeparatorStyle.None
            registerReusableCellClass(AddressCell::class.java, ADDRESS_CELL)
            rowHeight = UITableView.getAutomaticDimension()
            estimatedRowHeight = UITableView.getAutomaticDimension()
        }

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        uiLogic.init(getAppDelegate().database, walletId)
    }

    inner class AddressesDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            // TODO addresses +1
            return (uiLogic.addresses.size).toLong()
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            val cell = p0.dequeueReusableCell(ADDRESS_CELL)
            (cell as? AddressCell)?.let {
                it.bind(uiLogic.wallet!!, uiLogic.addresses[p1.row])
                it.clickListener = { addressId ->
                    // TODO addresses
                }
            }
            return cell
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

    inner class IosWalletAddressesUiLogic : WalletAddressesUiLogic() {
        var progressViewController: ProgressViewController? = null

        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun notifyNewAddresses() {
            runOnMainThread {
                tableView.reloadData()
            }
        }

        override fun notifyUiLocked(locked: Boolean) {
            runOnMainThread {
                if (locked) {
                    if (progressViewController == null) {
                        forceDismissKeyboard()
                        progressViewController = ProgressViewController()
                        progressViewController?.presentModalAbove(this@WalletAddressesViewController)
                    }
                } else {
                    progressViewController?.dismissViewController(false) {}
                    progressViewController = null
                }
            }
        }

    }
}