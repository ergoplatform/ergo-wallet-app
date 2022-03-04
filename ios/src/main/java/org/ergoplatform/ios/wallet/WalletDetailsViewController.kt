package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.getExplorerWebUrl
import org.ergoplatform.ios.tokens.TokenInformationViewController
import org.ergoplatform.ios.tokens.WalletDetailsTokenEntryView
import org.ergoplatform.ios.transactions.ColdWalletSigningViewController
import org.ergoplatform.ios.transactions.ErgoPaySigningViewController
import org.ergoplatform.ios.transactions.ReceiveToWalletViewController
import org.ergoplatform.ios.transactions.SendFundsViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.addresses.ChooseAddressListDialogViewController
import org.ergoplatform.ios.wallet.addresses.WalletAddressesViewController
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.WalletDetailsUiLogic
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.formatFiatToString
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

const val WIDTH_ICONS = 40.0

class WalletDetailsViewController(private val walletId: Int) : CoroutineViewController() {

    private lateinit var texts: I18NBundle
    private lateinit var addressContainer: AddressContainer
    private lateinit var balanceContainer: ErgoBalanceContainer
    private lateinit var tokenContainer: TokenListContainer
    private lateinit var tokenSeparator: UIView

    private val uiLogic = IosDetailsUiLogic()
    private var newDataLoaded: Boolean = false
    private var animateNextConfigRefresh = false

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
        tokenContainer = TokenListContainer()
        tokenSeparator = createHorizontalSeparator()
        val transactionsContainer = TransactionsContainer()

        val ownContainer = UIStackView(
            NSArray(
                addressContainer,
                createHorizontalSeparator(),
                balanceContainer,
                createHorizontalSeparator(),
                tokenContainer,
                tokenSeparator,
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
                WalletStateSyncManager.getInstance().refreshByUser(appDelegate.prefs, appDelegate.database)
            }
        }

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            // walletWithStateByIdAsFlow does not observe state and tokens, so we need observe
            // WalletStateSyncManager here too
            WalletStateSyncManager.getInstance().isRefreshing.collect { isRefreshing ->
                if (!isRefreshing && uiLogic.wallet != null) {
                    animateNextConfigRefresh = true
                    uiLogic.onWalletStateChanged(
                        getAppDelegate().database.walletDbProvider.loadWalletWithStateById(
                            walletId
                        ),
                        getAppDelegate().database.tokenDbProvider
                    )
                }
            }
        }
        uiLogic.setUpWalletStateFlowCollector(getAppDelegate().database, walletId)
        onResume()
    }

    override fun onResume() {
        val appDelegate = getAppDelegate()
        WalletStateSyncManager.getInstance().refreshWhenNeeded(
            appDelegate.prefs,
            appDelegate.database
        )
    }

    private fun refreshDataFromBackgroundThread() {
        newDataLoaded = true
        runOnMainThread {
            // usually, config changes are triggered by changes made on other screens (e.g. addresses list)
            // generally, there's no need to animate these changes. An exception is (un)folding tokens list:
            // this change should happen animated, as well as state change from WalletStateSyncManager
            if (animateNextConfigRefresh) {
                view.animateLayoutChanges { refresh() }
            } else
                refresh()
        }
    }

    private fun refresh() {
        if (uiLogic.wallet == null) {
            navigationController.popViewController(false)
        }

        animateNextConfigRefresh = false

        if (newDataLoaded)
            uiLogic.wallet?.let { wallet ->
                newDataLoaded = false
                LogUtils.logDebug("WalletDetailsViewController", "Refresh UI")
                title = wallet.walletConfig.displayName
                addressContainer.refresh()
                balanceContainer.refresh()
                tokenContainer.refresh()
                tokenSeparator.isHidden = tokenContainer.isHidden
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
            val scanQrButton =
                UIImageView(getIosSystemImage(IMAGE_QR_SCAN, UIImageSymbolScale.Small)).apply {
                    enforceKeepIntrinsicWidth()
                    tintColor = UIColor.label()
                    isUserInteractionEnabled = true
                    addGestureRecognizer(UITapGestureRecognizer {
                        presentViewController(QrScannerViewController(false) {
                            uiLogic.qrCodeScanned(it, IosStringProvider(texts), { data ->
                                navigationController.pushViewController(
                                    ColdWalletSigningViewController(
                                        data,
                                        walletId
                                    ), true
                                )
                            }, { ergoPayRequest ->
                                navigationController.pushViewController(
                                    ErgoPaySigningViewController(
                                        ergoPayRequest, walletId, uiLogic.addressIdx ?: -1
                                    ), true
                                )
                            }, { requestData ->
                                navigationController.pushViewController(
                                    SendFundsViewController(walletId, uiLogic.addressIdx ?: -1, requestData),
                                    true
                                )
                            }, { errorMessage ->
                                presentViewController(
                                    buildSimpleAlertController(
                                        "",
                                        errorMessage,
                                        texts
                                    ), true
                                ) {}
                            }
                            )
                        }, true) {}
                    })
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
                            uiLogic.newAddressIdxChosen(it)
                        }, true
                    ) {}
                })
            }

            val sendButton = UIImageView(getIosSystemImage(IMAGE_SEND, UIImageSymbolScale.Small)).apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                tintColor = UIColor.label()
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    navigationController.pushViewController(
                        SendFundsViewController(walletId, uiLogic.addressIdx ?: -1),
                        true
                    )
                })
            }
            val receiveButton = UIImageView(getIosSystemImage(IMAGE_RECEIVE, UIImageSymbolScale.Small)).apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                tintColor = UIColor.label()
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    navigationController.pushViewController(
                        ReceiveToWalletViewController(walletId, uiLogic.addressIdx ?: 0),
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
            addSubview(scanQrButton)
            addSubview(addressNameContainer)
            addSubview(horizontalStack)
            addressImage.leftToSuperview(inset = DEFAULT_MARGIN).topToSuperview().bottomToBottomOf(addressNameContainer)
            addressTitle.leftToRightOf(addressImage, DEFAULT_MARGIN * 2).topToSuperview()
                .rightToSuperview(inset = DEFAULT_MARGIN)
            scanQrButton.rightToSuperview(inset = -DEFAULT_MARGIN).centerVerticallyTo(addressTitle)
            addressNameContainer.leftToLeftOf(addressTitle).topToBottomOf(addressTitle, DEFAULT_MARGIN / 2)
                .rightToSuperview(inset = DEFAULT_MARGIN)
            horizontalStack.topToBottomOf(addressNameContainer, DEFAULT_MARGIN).rightToSuperview(inset = DEFAULT_MARGIN)
                .leftToLeftOf(addressNameContainer)
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN)
        }

        fun refresh() {
            addressNameLabel.text = uiLogic.getAddressLabel(IosStringProvider(texts))
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
            // fill balances
            val ergoAmount = uiLogic.getErgoBalance()
            balanceLabel.setErgoAmount(ergoAmount)

            val unconfirmed = uiLogic.getUnconfirmedErgoBalance()
            unconfirmedBalance.text = texts.format(STRING_LABEL_ERG_AMOUNT, unconfirmed.toStringRoundToDecimals()) +
                    " " + texts.get(STRING_LABEL_UNCONFIRMED)
            unconfirmedBalance.isHidden = (unconfirmed.isZero())

            // Fill fiat value
            val nodeConnector = WalletStateSyncManager.getInstance()
            val ergoPrice = nodeConnector.fiatValue.value
            fiatBalance.isHidden = ergoPrice == 0f

            fiatBalance.text = formatFiatToString(
                ergoPrice.toDouble() * ergoAmount.toDouble(),
                nodeConnector.fiatCurrency, IosStringProvider(texts)
            )
        }
    }

    inner class TokenListContainer : UIView(CGRect.Zero()) {
        private val tokensNumLabel = Headline2Label()
        private val tokensListStack = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Vertical
        }
        private val tokensDetailViewMap = HashMap<String, WalletDetailsTokenEntryView>()
        private val expandButton = UIImageView().apply {
            tintColor = UIColor.label()
        }

        init {
            val tokenImage = UIImageView(tokenLogoImage.imageWithTintColor(UIColor.secondaryLabel())).apply {
                contentMode = UIViewContentMode.ScaleAspectFit
                fixedWidth(WIDTH_ICONS)
                fixedHeight(WIDTH_ICONS)
            }
            val tokensTitle = Body1BoldLabel().apply {
                text = texts.get(STRING_LABEL_TOKENS)
                textColor = uiColorErgo
                numberOfLines = 1
            }

            addSubview(tokenImage)
            addSubview(tokensTitle)
            addSubview(tokensNumLabel)
            addSubview(tokensListStack)
            addSubview(expandButton)

            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                switchTokenVisibility()
            })

            tokenImage.leftToSuperview(inset = DEFAULT_MARGIN).topToSuperview()
            tokensNumLabel.leftToRightOf(tokenImage, DEFAULT_MARGIN * 2).centerVerticallyTo(tokenImage)
                .enforceKeepIntrinsicWidth()
            tokensTitle.leftToRightOf(tokensNumLabel, DEFAULT_MARGIN).centerVerticallyTo(tokensNumLabel)
            expandButton.rightToSuperview(inset = -DEFAULT_MARGIN).leftToRightOf(tokensTitle)
                .centerVerticallyTo(tokensTitle).enforceKeepIntrinsicWidth()
            tokensListStack.topToBottomOf(tokenImage).bottomToSuperview()
                .widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)
        }

        private fun switchTokenVisibility() {
            uiLogic.wallet?.walletConfig?.let { walletConfig ->
                viewControllerScope.launch {
                    animateNextConfigRefresh = true
                    getAppDelegate().database.walletDbProvider.updateWalletDisplayTokens(
                        !walletConfig.unfoldTokens,
                        walletId
                    )
                }
            }
        }

        fun refresh() {
            val tokensList = uiLogic.tokensList
            val infoHashMap = uiLogic.tokenInformation
            isHidden = tokensList.isEmpty()
            tokensNumLabel.text = tokensList.size.toString()

            tokensListStack.clearArrangedSubviews()
            tokensDetailViewMap.clear()
            val listExpanded = uiLogic.wallet?.walletConfig?.unfoldTokens == true
            if (listExpanded) {
                tokensList.forEach {
                    val tokenId = it.tokenId!!
                    val detailView = WalletDetailsTokenEntryView(it, texts) {
                        presentViewController(TokenInformationViewController(tokenId, it.amount), true) {}
                    }.bindWalletToken(infoHashMap[tokenId])
                    val container = UIView(CGRect.Zero()).apply {
                        addSubview(detailView)
                        minHeight(50.0)
                        detailView.widthMatchesSuperview().centerInSuperviewWhenSmaller()
                        layoutMargins = UIEdgeInsets.Zero()
                    }
                    tokensListStack.addArrangedSubview(container)
                    tokensDetailViewMap[tokenId] = detailView
                }
                val appDelegate = getAppDelegate()
                uiLogic.gatherTokenInformation(
                    appDelegate.database.tokenDbProvider, ErgoApiService.getOrInit(appDelegate.prefs)
                )
            }

            expandButton.image = getIosSystemImage(
                if (listExpanded) IMAGE_CHEVRON_UP else IMAGE_CHEVRON_DOWN,
                UIImageSymbolScale.Small,
                20.0
            )
        }

        fun addTokenInfo(tokenInformation: TokenInformation) {
            tokensDetailViewMap[tokenInformation.tokenId]?.bindWalletToken(tokenInformation)
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
                            (uiLogic.walletAddress?.publicAddress ?: uiLogic.wallet!!.walletConfig.firstAddress)
                )
            })
        }
    }


    inner class IosDetailsUiLogic : WalletDetailsUiLogic() {
        override val coroutineScope: CoroutineScope get() = viewControllerScope

        override fun onDataChanged() {
            refreshDataFromBackgroundThread()
        }

        override fun onNewTokenInfoGathered(tokenInformation: TokenInformation) {
            runOnMainThread { tokenContainer.addTokenInfo(tokenInformation) }
        }
    }
}