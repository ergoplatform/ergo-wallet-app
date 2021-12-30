package org.ergoplatform.ios.wallet.addresses

import org.ergoplatform.ErgoAmount
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_LABEL_WALLET_TOKEN_BALANCE
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.addresses.isDerivedAddress
import org.ergoplatform.wallet.getStateForAddress
import org.ergoplatform.wallet.getTokensForAddress
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

const val ADDRESS_CELL = "address_cell"

class AddressCell : AbstractTableViewCell(ADDRESS_CELL) {
    var clickListener: ((String) -> Unit)? = null

    private lateinit var addrIndexLabel: Headline1Label
    private lateinit var nameLabel: Body1BoldLabel
    private lateinit var publicAddressLabel: Body1Label
    private lateinit var separator: UIView
    private lateinit var ergAmount: ErgoAmountView
    private lateinit var tokenCount: Body1BoldLabel

    override fun setupView() {
        selectionStyle = UITableViewCellSelectionStyle.None
        addrIndexLabel = Headline1Label()
        nameLabel = Body1BoldLabel()
        publicAddressLabel = Body1Label()
        separator = createHorizontalSeparator()
        ergAmount = ErgoAmountView(true, FONT_SIZE_HEADLINE2)
        tokenCount = Body1BoldLabel()

        val ownContentView = UIView(CGRect.Zero())
        contentView.addSubview(ownContentView)
        ownContentView.edgesToSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        ownContentView.apply {
            addSubview(addrIndexLabel)
            addSubview(nameLabel)
            addSubview(publicAddressLabel)
            addSubview(separator)
        }

        addrIndexLabel.leftToSuperview(true)
        addrIndexLabel.apply {
            val centerConstraint = centerYAnchor.equalTo(
                publicAddressLabel.topAnchor,
                0.0
            )
            NSLayoutConstraint.activateConstraints(NSArray(centerConstraint))

            enforceKeepIntrinsicWidth()
            numberOfLines = 1
        }

        nameLabel.topToSuperview(true).leftToRightOf(addrIndexLabel, DEFAULT_MARGIN)
            .rightToSuperview(true, DEFAULT_MARGIN)
        nameLabel.apply {
            textColor = uiColorErgo
            numberOfLines = 1
        }

        publicAddressLabel.topToBottomOf(nameLabel).leftToRightOf(addrIndexLabel, DEFAULT_MARGIN)
            .rightToSuperview(true, DEFAULT_MARGIN)
        publicAddressLabel.apply {
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
        }

        separator.widthMatchesSuperview(true).topToBottomOf(publicAddressLabel, DEFAULT_MARGIN)

        val horizontalStack = UIView(CGRect.Zero()).apply {
            addSubview(ergAmount)
            addSubview(tokenCount)
        }
        contentView.addSubview(horizontalStack)
        ergAmount.leftToSuperview().topToSuperview().bottomToSuperview()
        tokenCount.leftToRightOf(ergAmount).centerVerticallyTo(ergAmount).rightToSuperview()
        horizontalStack.topToBottomOf(separator, DEFAULT_MARGIN)
            .bottomToSuperview(true, DEFAULT_MARGIN)
            .centerHorizontal()
    }

    fun bind(wallet: Wallet, walletAddress: WalletAddress) {
        val isDerivedAddress = walletAddress.isDerivedAddress()
        val texts = getAppDelegate().texts

        addrIndexLabel.text =
            if (isDerivedAddress) walletAddress.derivationIndex.toString() + " " else ""
        nameLabel.text = walletAddress.getAddressLabel(IosStringProvider(texts))
        publicAddressLabel.text = walletAddress.publicAddress

        val state = wallet.getStateForAddress(walletAddress.publicAddress)
        val tokens = wallet.getTokensForAddress(walletAddress.publicAddress)
        ergAmount.setErgoAmount(ErgoAmount(state?.balance ?: 0))
        tokenCount.isHidden = tokens.isEmpty()
        tokenCount.text = if (tokens.isEmpty()) "" else "   " + // some margin blanks
                texts.format(STRING_LABEL_WALLET_TOKEN_BALANCE, tokens.size.toString())
    }

}