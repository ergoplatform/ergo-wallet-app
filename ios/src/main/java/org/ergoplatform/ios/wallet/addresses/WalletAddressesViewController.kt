package org.ergoplatform.ios.wallet.addresses

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_BUTTON_ADD_ADDRESS
import org.ergoplatform.uilogic.STRING_BUTTON_ADD_ADDRESSES
import org.ergoplatform.uilogic.STRING_DESC_WALLET_ADDRESSES
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_ADDRESSES
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressesUiLogic
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.addresses.isDerivedAddress
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*
import kotlin.math.roundToInt

const val ADD_ADDR_CELL = "add_addr_cell"

class WalletAddressesViewController(private val walletId: Int) : CoroutineViewController() {
    private val uiLogic = IosWalletAddressesUiLogic()

    private val tableView = UITableView(CGRect.Zero())
    private val walletTitle = Body1BoldLabel()

    override fun viewDidLoad() {
        super.viewDidLoad()

        title = getAppDelegate().texts.get(STRING_TITLE_WALLET_ADDRESSES)

        tableView.tableHeaderView = UIView(CGRect(0.0, 0.0, 0.0, 35.0)).apply {
            addSubview(walletTitle)
            walletTitle.edgesToSuperview()
            walletTitle.textAlignment = NSTextAlignment.Center
            walletTitle.textColor = uiColorErgo
        }

        view.addSubview(tableView)
        tableView.edgesToSuperview(false)

        tableView.apply {

            dataSource = AddressesDataSource()
            separatorStyle = UITableViewCellSeparatorStyle.SingleLine
            registerReusableCellClass(ConfigListAddressCell::class.java, ADDRESS_CELL)
            registerReusableCellClass(AddAddressCell::class.java, ADD_ADDR_CELL)
            rowHeight = UITableView.getAutomaticDimension()
            estimatedRowHeight = UITableView.getAutomaticDimension()
        }

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        // balance refresh for newly added addresses needs to be triggered by
        // observing NodeConnector refresh. Since the singleAddressRefresh will
        // always return its last saved value, initializing can be done here, too
        viewControllerScope.launch {
            val nodeConnector = WalletStateSyncManager.getInstance()
            nodeConnector.singleAddressRefresh.collect {
                uiLogic.init(getAppDelegate().database.walletDbProvider, walletId)
            }
        }
    }

    inner class AddressesDataSource : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            return (uiLogic.addresses.size + 1).toLong()
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            val index = p1.row
            return if (index < uiLogic.addresses.size) {
                val cell = p0.dequeueReusableCell(ADDRESS_CELL)
                (cell as? ConfigListAddressCell)?.let {
                    val walletAddress = uiLogic.addresses[index]
                    it.bind(uiLogic.wallet!!, walletAddress)
                    it.clickListener = if (walletAddress.isDerivedAddress()) {
                        { address ->
                            presentViewController(WalletAddressDialogViewController(address), true) {}
                        }
                    } else
                        null
                }
                cell
            } else {
                val cell = p0.dequeueReusableCell(ADD_ADDR_CELL)
                (cell as? AddAddressCell)?.bind(this@WalletAddressesViewController)
                cell
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

    inner class IosWalletAddressesUiLogic : WalletAddressesUiLogic() {
        private val progressViewController =
            ProgressViewController.ProgressViewControllerPresenter(this@WalletAddressesViewController)

        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun notifyNewAddresses() {
            runOnMainThread {
                walletTitle.text = wallet?.walletConfig?.displayName ?: ""
                tableView.reloadData()
            }
        }

        override fun notifyUiLocked(locked: Boolean) {
            runOnMainThread {
                progressViewController.setUiLocked(locked)
            }
        }

    }

    class AddAddressCell : AbstractTableViewCell(ADD_ADDR_CELL) {
        private var parentVc: WalletAddressesViewController? = null
        private lateinit var addButton: PrimaryButton
        private var addrCount = 1

        override fun setupView() {
            selectionStyle = UITableViewCellSelectionStyle.None
            val texts = getAppDelegate().texts

            val description = Body1Label().apply {
                text = texts.get(STRING_DESC_WALLET_ADDRESSES)
                textAlignment = NSTextAlignment.Center
            }

            addrCount = 1
            val slider = UISlider(CGRect.Zero()).apply {
                maximumValue = 9f
                minimumValue = 0f
                isContinuous = false
                addOnValueChangedListener {
                    val rounded = value.roundToInt()
                    value = rounded.toFloat()
                    addrCount = rounded + 1
                    refreshButtonText(texts)
                }
            }

            addButton = PrimaryButton(texts.get(STRING_BUTTON_ADD_ADDRESS))
            addButton.addOnTouchUpInsideListener { _, _ ->
                parentVc?.uiLogic?.wallet?.walletConfig?.let { walletConfig ->
                    walletConfig.secretStorage?.let {
                        parentVc?.startAuthFlow(walletConfig) { mnemonic ->
                            addAddresses(mnemonic)
                        }
                    } ?: addAddresses(null)
                }
            }

            val ownContentView = UIView(CGRect.Zero()).apply {
                addSubview(description)
                addSubview(slider)
                addSubview(addButton)
            }

            description.topToSuperview().widthMatchesSuperview()
            slider.topToBottomOf(description, inset = DEFAULT_MARGIN)
                .widthMatchesSuperview(inset = DEFAULT_MARGIN * 3)
            addButton.topToBottomOf(slider, DEFAULT_MARGIN * 2).bottomToSuperview()
                .widthMatchesSuperview()

            contentView.addSubview(ownContentView)
            ownContentView.edgesToSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        }

        private fun addAddresses(mnemonic: String?) {
            LogUtils.logDebug("WalletAddressesVc", "Adding $addrCount addresses")
            val appDelegate = getAppDelegate()
            parentVc!!.uiLogic.addNextAddresses(
                appDelegate.database.walletDbProvider,
                appDelegate.prefs, addrCount, mnemonic
            )
        }

        private fun refreshButtonText(texts: I18NBundle) {
            addButton.setTitle(
                if (addrCount == 1) texts.get(STRING_BUTTON_ADD_ADDRESS)
                else texts.format(STRING_BUTTON_ADD_ADDRESSES, addrCount),
                UIControlState.Normal
            )
        }

        fun bind(vc: WalletAddressesViewController) {
            addButton.isEnabled = vc.uiLogic.canDeriveAddresses()
            this.parentVc = vc
        }
    }
}