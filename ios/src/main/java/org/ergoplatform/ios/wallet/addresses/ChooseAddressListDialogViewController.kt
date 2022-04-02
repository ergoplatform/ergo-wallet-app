package org.ergoplatform.ios.wallet.addresses

import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_TITLE_CHOOSE_ADDRESS
import org.ergoplatform.uilogic.wallet.addresses.ChooseAddressListAdapterLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*

class ChooseAddressListDialogViewController(
    private val walletId: Int,
    private val showAllAddresses: Boolean,
    private val addressChosen: (Int?) -> Unit
) : CoroutineViewController() {

    private var adapterLogic: ChooseAddressListAdapterLogic? = null
    private lateinit var tableView: UITableView

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        addCloseButton()

        val titleLabel = Body1Label().apply {
            text = getAppDelegate().texts.get(STRING_TITLE_CHOOSE_ADDRESS)
            textAlignment = NSTextAlignment.Center
        }

        view.addSubview(titleLabel)

        titleLabel.topToSuperview(topInset = DEFAULT_MARGIN * 2)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN)

        tableView = UITableView(CGRect.Zero())
        view.addSubview(tableView)

        tableView.apply {
            widthMatchesSuperview().bottomToSuperview().topToBottomOf(titleLabel, DEFAULT_MARGIN * 2)

            dataSource = AddressesDataSource()
            registerReusableCellClass(ChooseAddressCell::class.java, ADDRESS_CELL)
            rowHeight = UITableView.getAutomaticDimension()
            estimatedRowHeight = UITableView.getAutomaticDimension()
            tableFooterView = UIView(CGRect.Zero()) // causes iOS to hide trailing empty cells
        }

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            getAppDelegate().database.walletDbProvider.loadWalletWithStateById(walletId)?.let {
                adapterLogic = ChooseAddressListAdapterLogic(it, showAllAddresses)
                runOnMainThread {
                    tableView.reloadData()
                }
            }
        }
    }

    inner class AddressesDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            // When we already have data to show, show the empty cell if no wallets configured
            return (adapterLogic?.itemCount ?: 0).toLong()
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            val cell = p0.dequeueReusableCell(ADDRESS_CELL)
            (cell as? ChooseAddressCell)?.let {
                adapterLogic?.bindViewHolder(it, p1.row)
                it.clickListener = { address ->
                    addressChosen.invoke(address?.derivationIndex)
                    dismissViewController(true) {}
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
}