package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
import org.ergoplatform.NodeConnector
import org.ergoplatform.getExplorerWebUrl
import org.ergoplatform.ios.transactions.ReceiveToWalletViewController
import org.ergoplatform.ios.transactions.SendFundsViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.addresses.ChooseAddressListDialogViewController
import org.ergoplatform.ios.wallet.addresses.WalletAddressesViewController
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.wallet.*
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

const val WIDTH_ICONS = 40.0

class WalletDetailsViewController(private val walletId: Int) : CoroutineViewController() {

    private lateinit var texts: I18NBundle
    private lateinit var addressContainer: AddressContainer
    private lateinit var balanceContainer: ErgoBalanceContainer

    private var wallet: Wallet? = null
    private var addressIdx: Int? = null
    private var walletAddress: WalletAddress? = null
    private var newDataLoaded: Boolean = false

    override fun viewDidLoad() {
        super.viewDidLoad()

        texts = getAppDelegate().texts
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val walletConfigButton =
            UIBarButtonItem(getIosSystemImage(IMAGE_SETTINGS, UIImageSymbolScale.Small), UIBarButtonItemStyle.Plain)
        navigationItem.rightBarButtonItem = walletConfigButton
        walletConfigButton.tintColor = UIColor.label()
        walletConfigButton.setOnClickListener {
            navigationController.pushViewController(WalletConfigViewController(walletId), true)
        }

        addressContainer = AddressContainer()
        balanceContainer = ErgoBalanceContainer()
        val transactionsContainer = TransactionsContainer()

        val ownContainer = UIStackView(
            NSArray(
                addressContainer,
                createHorizontalSeparator(),
                balanceContainer,
                createHorizontalSeparator(),
                transactionsContainer
            )
        ).apply {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN
        }
        val scrollView = ownContainer.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.edgesToSuperview(maxWidth = MAX_WIDTH)

        val uiRefreshControl = UIRefreshControl()
        scrollView.refreshControl = uiRefreshControl
        uiRefreshControl.addOnValueChangedListener {
            if (uiRefreshControl.isRefreshing) {
                uiRefreshControl.endRefreshing()
                val appDelegate = getAppDelegate()
                NodeConnector.getInstance().refreshByUser(appDelegate.prefs, appDelegate.database)
            }
        }

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            NodeConnector.getInstance().isRefreshing.collect { isRefreshing ->
                if (!isRefreshing && wallet != null) {
                    wallet = getAppDelegate().database.loadWalletWithStateById(walletId)
                    newDataLoaded = true
                    runOnMainThread {
                        view.animateLayoutChanges { refresh() }
                    }
                }
            }
        }
        viewControllerScope.launch {
            getAppDelegate().database.walletWithStateByIdAsFlow(walletId).collect {
                wallet = it
                newDataLoaded = true

                if (addressIdx == null && wallet?.getNumOfAddresses() == 1) {
                    addressIdx = 0
                }

                runOnMainThread {
                    refresh()
                }
            }
        }
        onResume()
    }

    override fun onResume() {
        val appDelegate = getAppDelegate()
        NodeConnector.getInstance().refreshWhenNeeded(
            appDelegate.prefs,
            appDelegate.database
        )
    }

    fun refresh() {
        // TODO leave screen when wallet is null (removed)

        if (newDataLoaded)
            wallet?.let { wallet ->
                newDataLoaded = false
                LogUtils.logDebug("WalletDetailsViewController", "Refresh UI")
                walletAddress = addressIdx?.let { wallet.getDerivedAddressEntity(it) }
                title = wallet.walletConfig.displayName
                addressContainer.refresh()
                balanceContainer.refresh()
            }
    }

    inner class AddressContainer : UIView(CGRect.Zero()) {
        private val addressNameLabel = Body1BoldLabel()

        init {
            val addressImage = UIImageView(getIosSystemImage(IMAGE_ADDRESS, UIImageSymbolScale.Medium)).apply {
                tintColor = UIColor.secondaryLabel()
                contentMode = UIViewContentMode.Center
                fixedWidth(WIDTH_ICONS)
            }
            val addressTitle = Body1BoldLabel().apply {
                text = texts.get(STRING_TITLE_WALLET_ADDRESS)
            }

            addressNameLabel.apply {
                numberOfLines = 1
                textColor = uiColorErgo
            }
            val addressNameContainer = addressNameLabel.wrapWithTrailingImage(
                getIosSystemImage(
                    IMAGE_OPEN_LIST,
                    UIImageSymbolScale.Small,
                    20.0
                )!!
            ).apply {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    presentViewController(
                        ChooseAddressListDialogViewController(walletId, true) {
                            addressIdx = it
                            newDataLoaded = true
                            this@WalletDetailsViewController.refresh()
                        }, true
                    ) {}
                })
            }

            val sendButton = UIImageView(getIosSystemImage(IMAGE_SEND, UIImageSymbolScale.Small)).apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                tintColor = UIColor.label()
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    navigationController.pushViewController(SendFundsViewController(walletId, addressIdx ?: -1), true)
                })
            }
            val receiveButton = UIImageView(getIosSystemImage(IMAGE_RECEIVE, UIImageSymbolScale.Small)).apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                tintColor = UIColor.label()
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    navigationController.pushViewController(
                        ReceiveToWalletViewController(walletId, addressIdx ?: 0),
                        true
                    )
                })
            }
            val addressListButton =
                UIImageView(getIosSystemImage(IMAGE_ADDRESS_LIST, UIImageSymbolScale.Small)).apply {
                    contentMode = UIViewContentMode.ScaleAspectFit
                    tintColor = UIColor.label()
                    isUserInteractionEnabled = true
                    addGestureRecognizer(UITapGestureRecognizer {
                        navigationController.pushViewController(WalletAddressesViewController(walletId), true)
                    })
                }

            val horizontalStack = UIStackView(NSArray(receiveButton, sendButton, addressListButton)).apply {
                axis = UILayoutConstraintAxis.Horizontal
                distribution = UIStackViewDistribution.FillEqually
            }

            addSubview(addressImage)
            addSubview(addressTitle)
            addSubview(addressNameContainer)
            addSubview(horizontalStack)
            addressImage.leftToSuperview(inset = DEFAULT_MARGIN).topToSuperview().bottomToBottomOf(addressNameContainer)
            addressTitle.leftToRightOf(addressImage, DEFAULT_MARGIN * 2).topToSuperview()
                .rightToSuperview(inset = DEFAULT_MARGIN)
            addressNameContainer.leftToLeftOf(addressTitle).topToBottomOf(addressTitle, DEFAULT_MARGIN / 2)
                .rightToSuperview(inset = DEFAULT_MARGIN)
            horizontalStack.topToBottomOf(addressNameContainer, DEFAULT_MARGIN).rightToSuperview(inset = DEFAULT_MARGIN)
                .leftToLeftOf(addressNameContainer)
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN)
        }

        fun refresh() {
            addressNameLabel.text = walletAddress?.getAddressLabel(IosStringProvider(texts))
                ?: texts.format(STRING_LABEL_ALL_ADDRESSES, wallet!!.getNumOfAddresses())
        }
    }

    inner class ErgoBalanceContainer : UIView(CGRect.Zero()) {
        private val balanceLabel = ErgoAmountView(true, FONT_SIZE_HEADLINE1)
        private val fiatBalance = Body1Label().apply {
            textColor = UIColor.secondaryLabel()
        }

        private val unconfirmedBalance = Body1BoldLabel().apply {
            numberOfLines = 1
        }

        init {
            val ergoCoinImage = UIImageView(ergoLogoFilledImage.imageWithTintColor(UIColor.secondaryLabel())).apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                fixedWidth(WIDTH_ICONS)
            }
            val balanceTitle = Body1BoldLabel().apply {
                text = texts.get(STRING_TITLE_WALLET_BALANCE)
                textColor = uiColorErgo
            }

            val stackView = UIStackView(
                NSArray(
                    balanceTitle,
                    balanceLabel,
                    fiatBalance,
                    unconfirmedBalance
                )
            ).apply {
                alignment = UIStackViewAlignment.Leading
                axis = UILayoutConstraintAxis.Vertical
            }

            addSubview(ergoCoinImage)
            addSubview(stackView)

            ergoCoinImage.leftToSuperview(inset = DEFAULT_MARGIN).centerVerticallyTo(balanceLabel)
            stackView.leftToRightOf(ergoCoinImage, DEFAULT_MARGIN * 2).rightToSuperview().superViewWrapsHeight()
        }

        fun refresh() {
            wallet?.let { wallet ->

                // fill balances
                val addressState = walletAddress?.let { wallet.getStateForAddress(it.publicAddress) }
                val ergoAmount = ErgoAmount(addressState?.balance ?: wallet.getBalanceForAllAddresses())
                balanceLabel.setErgoAmount(ergoAmount)

                val unconfirmed = addressState?.unconfirmedBalance ?: wallet.getUnconfirmedBalanceForAllAddresses()
                unconfirmedBalance.text =
                    texts.format(STRING_LABEL_ERG_AMOUNT, ErgoAmount(unconfirmed).toStringRoundToDecimals()) +
                            " " + texts.get(STRING_LABEL_UNCONFIRMED)
                unconfirmedBalance.isHidden = (unconfirmed == 0L)

                // Fill fiat value
                val nodeConnector = NodeConnector.getInstance()
                val ergoPrice = nodeConnector.fiatValue.value
                fiatBalance.isHidden = ergoPrice == 0f

                fiatBalance.text = formatFiatToString(
                    ergoPrice.toDouble() * ergoAmount.toDouble(),
                    nodeConnector.fiatCurrency, IosStringProvider(texts)
                )
            }
        }
    }

    inner class TransactionsContainer : CardView() {
        init {
            val transactionsImage =
                UIImageView(getIosSystemImage(IMAGE_TRANSACTIONS, UIImageSymbolScale.Medium)).apply {
                    tintColor = UIColor.secondaryLabel()
                    contentMode = UIViewContentMode.Center
                    fixedWidth(WIDTH_ICONS)
                }
            val transactionsTitle = Body1BoldLabel().apply {
                text = texts.get(STRING_TITLE_TRANSACTIONS)
                textColor = uiColorErgo
            }

            val transactionsDesc = Body1Label().apply {
                text = texts.get(STRING_EXPORER_VIEW_TRANSACTIONS)
            }

            contentView.apply {
                addSubview(transactionsImage)
                addSubview(transactionsTitle)
                addSubview(transactionsDesc)
            }

            transactionsImage.leftToSuperview(inset = DEFAULT_MARGIN).topToSuperview(topInset = DEFAULT_MARGIN)
            transactionsTitle.leftToRightOf(transactionsImage, DEFAULT_MARGIN * 2).topToTopOf(transactionsImage)
                .rightToSuperview(inset = DEFAULT_MARGIN)
            transactionsDesc.leftToLeftOf(transactionsTitle).rightToSuperview(inset = DEFAULT_MARGIN)
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN).topToBottomOf(transactionsTitle, DEFAULT_MARGIN)

            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                openUrlInBrowser(
                    getExplorerWebUrl() + "en/addresses/" +
                            (walletAddress?.publicAddress ?: wallet!!.walletConfig.firstAddress)
                )
            })
        }
    }
}