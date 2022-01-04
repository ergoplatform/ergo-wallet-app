package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.ios.transactions.ReceiveToWalletViewController
import org.ergoplatform.ios.transactions.SendFundsViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.addresses.ChooseAddressListDialogViewController
import org.ergoplatform.ios.wallet.addresses.WalletAddressesViewController
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_LABEL_ALL_ADDRESSES
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_ADDRESS
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getDerivedAddressEntity
import org.ergoplatform.wallet.getNumOfAddresses
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class WalletDetailsViewController(private val walletId: Int) : CoroutineViewController() {

    private lateinit var texts: I18NBundle
    private lateinit var addressContainer: AddressContainer

    private var wallet: Wallet? = null
    private var addressIdx: Int? = null

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
        view.addSubview(addressContainer)

        addressContainer.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            NodeConnector.getInstance().isRefreshing.collect {
                wallet = getAppDelegate().database.loadWalletWithStateById(walletId)
                runOnMainThread {
                    refresh()
                }
            }
        }
        viewControllerScope.launch {
            getAppDelegate().database.walletWithStateByIdAsFlow(walletId).collect {
                wallet = it

                if (addressIdx == null && wallet?.getNumOfAddresses() == 1) {
                    addressIdx = 0
                }

                runOnMainThread {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        // TODO leave screen when wallet is null (removed)

        wallet?.let { wallet ->
            title = wallet.walletConfig.displayName
            addressContainer.refresh()
        }
    }

    inner class AddressContainer : UIView(CGRect.Zero()) {
        private val addressNameLabel = Body1BoldLabel()

        init {
            layoutMargins = UIEdgeInsets.Zero()

            val addressImage = UIImageView(getIosSystemImage(IMAGE_ADDRESS, UIImageSymbolScale.Medium)).apply {
                tintColor = UIColor.secondaryLabel()
                contentMode = UIViewContentMode.Center
                enforceKeepIntrinsicWidth()
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
            addressImage.leftToSuperview().topToSuperview().bottomToBottomOf(addressNameContainer)
            addressTitle.leftToRightOf(addressImage, DEFAULT_MARGIN).topToSuperview()
                .rightToSuperview(inset = DEFAULT_MARGIN)
            addressNameContainer.leftToLeftOf(addressTitle).topToBottomOf(addressTitle)
                .rightToSuperview(inset = DEFAULT_MARGIN)
            horizontalStack.topToBottomOf(addressNameContainer, DEFAULT_MARGIN).rightToSuperview(inset = DEFAULT_MARGIN)
                .leftToLeftOf(addressNameContainer)
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN)
        }

        fun refresh() {
            addressNameLabel.text =
                addressIdx?.let { wallet?.getDerivedAddressEntity(it)?.getAddressLabel(IosStringProvider(texts)) }
                    ?: texts.format(STRING_LABEL_ALL_ADDRESSES, wallet!!.getNumOfAddresses())
        }
    }
}