package org.ergoplatform.ios.wallet.addresses

import org.ergoplatform.ErgoAmount
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_TOKEN_BALANCE
import org.ergoplatform.uilogic.wallet.addresses.ChooseAddressListAdapterLogic
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.addresses.isDerivedAddress
import org.ergoplatform.wallet.getStateForAddress
import org.ergoplatform.wallet.getTokensForAddress
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

const val ADDRESS_CELL = "address_cell"

abstract class AddressCell : AbstractTableViewCell(ADDRESS_CELL) {
    open var clickListener: ((WalletAddress) -> Unit)? = null
    abstract val showSeparator: Boolean

    protected lateinit var addressIndexLabel: Headline1Label
        private set
    protected lateinit var nameLabel: Body1BoldLabel
        private set
    protected lateinit var publicAddressLabel: Body1Label
        private set
    private var separator: UIView? = null
    private lateinit var ergAmount: ErgoAmountView
    private lateinit var tokenCount: Body1BoldLabel
    protected lateinit var ownContentView: UIView
        private set

    private var walletAddress: WalletAddress? = null

    override fun setupView() {
        selectionStyle = UITableViewCellSelectionStyle.None
        addressIndexLabel = Headline1Label()
        nameLabel = Body1BoldLabel()
        publicAddressLabel = Body1Label()
        if (showSeparator) {
            separator = createHorizontalSeparator()
        }
        ergAmount = ErgoAmountView(true, FONT_SIZE_HEADLINE2)
        tokenCount = Body1BoldLabel()

        ownContentView = UIView(CGRect.Zero())
        contentView.addSubview(ownContentView)
        ownContentView.edgesToSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        ownContentView.apply {
            addSubview(addressIndexLabel)
            addSubview(nameLabel)
            addSubview(publicAddressLabel)
            separator?.let { addSubview(separator) }
        }

        addressIndexLabel.leftToSuperview(true)
        addressIndexLabel.apply {
            val centerConstraint = centerYAnchor.equalTo(
                publicAddressLabel.topAnchor,
                0.0
            )
            NSLayoutConstraint.activateConstraints(NSArray(centerConstraint))

            enforceKeepIntrinsicWidth()
            numberOfLines = 1
        }

        nameLabel.topToSuperview(true).leftToRightOf(addressIndexLabel, DEFAULT_MARGIN)
            .rightToSuperview(true, DEFAULT_MARGIN)
        nameLabel.apply {
            textColor = uiColorErgo
            numberOfLines = 1
        }

        publicAddressLabel.topToBottomOf(nameLabel).leftToRightOf(addressIndexLabel, DEFAULT_MARGIN)
            .rightToSuperview(true, DEFAULT_MARGIN)
        publicAddressLabel.apply {
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
        }

        separator?.widthMatchesSuperview(true)?.topToBottomOf(publicAddressLabel, DEFAULT_MARGIN)

        val horizontalStack = UIView(CGRect.Zero()).apply {
            addSubview(ergAmount)
            addSubview(tokenCount)
        }
        contentView.addSubview(horizontalStack)
        ergAmount.leftToSuperview().topToSuperview().bottomToSuperview()
        tokenCount.leftToRightOf(ergAmount).centerVerticallyTo(ergAmount).rightToSuperview()
        horizontalStack.topToBottomOf(separator ?: publicAddressLabel)
            .bottomToSuperview(true, DEFAULT_MARGIN)
            .centerHorizontal()

        contentView.isUserInteractionEnabled = true
        contentView.addGestureRecognizer(UITapGestureRecognizer {
            walletAddress?.let { clickListener?.invoke(it) }
        })
    }

    open fun bind(wallet: Wallet, walletAddress: WalletAddress) {
        val texts = getAppDelegate().texts

        nameLabel.text = walletAddress.getAddressLabel(IosStringProvider(texts))

        val state = wallet.getStateForAddress(walletAddress.publicAddress)
        val tokens = wallet.getTokensForAddress(walletAddress.publicAddress)
        ergAmount.setErgoAmount(ErgoAmount(state?.balance ?: 0))
        tokenCount.isHidden = tokens.isEmpty()
        tokenCount.text = if (tokens.isEmpty()) "" else "   " + // some margin blanks
                texts.format(STRING_LABEL_WALLET_TOKEN_BALANCE, tokens.size.toString())

        this.walletAddress = walletAddress
    }

}

class ConfigListAddressCell : AddressCell() {
    private lateinit var moreActionButton: UIImageView

    override var clickListener: ((WalletAddress) -> Unit)? = null
        set(value) {
            field = value
            moreActionButton.isHidden = value == null
        }

    override val showSeparator get() = true

    override fun setupView() {
        super.setupView()

        moreActionButton = UIImageView(getIosSystemImage(IMAGE_MORE_ACTION, UIImageSymbolScale.Small)).apply {
            tintColor = UIColor.label()
        }
        contentView.addSubview(moreActionButton)
        moreActionButton.topToSuperview().leftToRightOf(ownContentView, -5.0).fixedWidth(20.0).fixedHeight(20.0)

    }

    override fun bind(wallet: Wallet, walletAddress: WalletAddress) {
        super.bind(wallet, walletAddress)

        val isDerivedAddress = walletAddress.isDerivedAddress()
        addressIndexLabel.text =
            if (isDerivedAddress) walletAddress.derivationIndex.toString() + " " else ""
        publicAddressLabel.text = walletAddress.publicAddress
    }
}

class ChooseAddressCell : AddressCell(), ChooseAddressListAdapterLogic.AddressHolder {
    override val showSeparator get() = false

    override fun setupView() {
        super.setupView()
        nameLabel.textAlignment = NSTextAlignment.Center
    }

    override fun bindAddress(address: WalletAddress, wallet: Wallet) {
        bind(wallet, address)
    }

    override fun bindAllAddresses(wallet: Wallet) {
        // TODO addresses implement
    }

}