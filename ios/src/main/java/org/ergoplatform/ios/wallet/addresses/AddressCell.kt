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
import org.robovm.apple.uikit.NSLineBreakMode
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UIView

class AddressCell : AbstractTableViewCell(ADDRESS_CELL) {
    var clickListener: ((String) -> Unit)? = null

    private lateinit var addrIndexLabel: Headline1Label
    private lateinit var nameLabel: Body1BoldLabel
    private lateinit var publicAddressLabel: Body1Label
    private lateinit var separator: UIView
    private lateinit var ergAmount: ErgoAmountView
    private lateinit var tokenCount: Body1BoldLabel

    override fun setupView() {
        addrIndexLabel = Headline1Label()
        nameLabel = Body1BoldLabel()
        publicAddressLabel = Body1Label()
        separator = createHorizontalSeparator()
        ergAmount = ErgoAmountView(true, FONT_SIZE_HEADLINE2)
        tokenCount = Body1BoldLabel()

        val ownContentView = UIView(CGRect.Zero())
        contentView.addSubview(ownContentView)
        ownContentView.topToSuperview(topInset = DEFAULT_MARGIN)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        ownContentView.apply {
            addSubview(addrIndexLabel)
            addSubview(nameLabel)
            addSubview(publicAddressLabel)
            addSubview(separator)
        }

        addrIndexLabel.leftToSuperview(true).topToSuperview(true)
        addrIndexLabel.apply {
            enforceKeepIntrinsicWidth()
            numberOfLines = 1
        }

        nameLabel.topToSuperview(true).leftToRightOf(addrIndexLabel).rightToSuperview(true)
        nameLabel.apply {
            textColor = uiColorErgo
            numberOfLines = 1
        }

        publicAddressLabel.topToBottomOf(nameLabel).leftToRightOf(addrIndexLabel)
            .rightToSuperview(true)
        publicAddressLabel.apply {
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
        }

        separator.widthMatchesSuperview(true).topToBottomOf(publicAddressLabel, DEFAULT_MARGIN)

        val horizontalStack = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Horizontal
            spacing = 4 * DEFAULT_MARGIN
            addArrangedSubview(ergAmount)
            addArrangedSubview(tokenCount)
        }
        contentView.addSubview(horizontalStack)
        horizontalStack.topToBottomOf(separator, DEFAULT_MARGIN).bottomToSuperview(true)
            .centerHorizontal()
    }

    fun bind(wallet: Wallet, walletAddress: WalletAddress) {
        val isDerivedAddress = walletAddress.isDerivedAddress()
        val texts = getAppDelegate().texts

        addrIndexLabel.text =
            if (isDerivedAddress) walletAddress.derivationIndex.toString() else ""
        nameLabel.text = walletAddress.getAddressLabel(IosStringProvider(texts))
        publicAddressLabel.text = walletAddress.publicAddress

        val state = wallet.getStateForAddress(walletAddress.publicAddress)
        val tokens = wallet.getTokensForAddress(walletAddress.publicAddress)
        ergAmount.setErgoAmount(ErgoAmount(state?.balance ?: 0))
        tokenCount.isHidden = tokens.isNullOrEmpty()
        tokenCount.text = texts.format(STRING_LABEL_WALLET_TOKEN_BALANCE, tokens.size.toString())
    }

}