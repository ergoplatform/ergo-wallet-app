package org.ergoplatform.ios.wallet

import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletConfig
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSIndexPath
import org.robovm.apple.uikit.*
import org.robovm.objc.annotation.CustomClass
import kotlin.math.max

class WalletViewController : UIViewController() {

    private val tableView = UITableView(CGRect.Zero())
    private val shownData = ArrayList<WalletConfig>()

    override fun viewDidLoad() {
        val addWalletButton = UIBarButtonItem(UIBarButtonSystemItem.Add)
        navigationItem.rightBarButtonItem = addWalletButton
        addWalletButton.tintColor = UIColor.label()
        addWalletButton.setOnClickListener {
            val navController = UINavigationController(AddWalletChooserViewController())
            navController.modalPresentationStyle = UIModalPresentationStyle.FormSheet
            navController.isModalInPresentation = true
            this.presentViewController(navController, true, {})
        }

        view.addSubview(tableView)
        tableView.edgesToSuperview(true)

        tableView.dataSource = WalletDataSource()
        tableView.separatorStyle = UITableViewCellSeparatorStyle.None
        tableView.refreshControl = UIRefreshControl()
        tableView.registerReusableCellClass(WalletCell::class.java, WALLET_CELL)
        tableView.registerReusableCellClass(EmptyCell::class.java, EMPTY_CELL)
        tableView.rowHeight = UITableView.getAutomaticDimension()
        tableView.estimatedRowHeight = UITableView.getAutomaticDimension()

        tableView.tableHeaderView = HeaderView()
        tableView.tableHeaderView.backgroundColor = UIColor.secondarySystemBackground()
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

    inner class WalletDataSource() : UITableViewDataSourceAdapter() {
        override fun getNumberOfRowsInSection(p0: UITableView?, p1: Long): Long {
            return max(1, shownData.size.toLong())
        }

        override fun getCellForRow(p0: UITableView, p1: NSIndexPath): UITableViewCell {
            if (shownData.isEmpty()) {
                val cell = p0.dequeueReusableCell(EMPTY_CELL)
                (cell as? EmptyCell)?.walletChooserStackView?.clickListener = {
                    val navController = UINavigationController(RestoreWalletViewController())
                    navController.modalPresentationStyle = UIModalPresentationStyle.FormSheet
                    navController.isModalInPresentation = true
                    presentViewController(navController, true, {})
                }
                return cell
            } else {
                val cell = p0.dequeueReusableCell(WALLET_CELL)
                (cell as? WalletCell)?.bind(shownData.get(p1.row))
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
        val ergoLogo: UIImageView
        val fiatLabel = Body1Label()
        val syncLabel = Body1Label()

        init {
            val image = UIImage.getImage("ergologo")
            ergoLogo = UIImageView(image.imageWithTintColor(UIColor.label()))
            ergoLogo.contentMode = UIViewContentMode.ScaleAspectFit

            val stackview = UIStackView(NSArray(fiatLabel, syncLabel))
            stackview.axis = UILayoutConstraintAxis.Vertical
            addSubview(stackview)
            addSubview(ergoLogo)
            ergoLogo.leftToSuperview().topToSuperview().bottomToSuperview()
            stackview.topToSuperview().bottomToSuperview().leftToRightOf(ergoLogo)
            NSLayoutConstraint.activateConstraints(NSArray(ergoLogo.widthAnchor.equalTo(frame.height)))

            fiatLabel.text = getAppDelegate().texts.get(STRING_LABEL_ERG_PRICE)
            syncLabel.text = getAppDelegate().texts.get(STRING_LABEL_LAST_SYNC)
        }
    }

}