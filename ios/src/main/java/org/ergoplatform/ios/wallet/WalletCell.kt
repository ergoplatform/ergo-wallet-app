package org.ergoplatform.ios.wallet

import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletConfig
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.*

const val WALLET_CELL = "WalletCell"

class WalletCell : UITableViewCell(UITableViewCellStyle.Default, WALLET_CELL) {
    private lateinit var nameLabel: Body1Label
    private lateinit var balanceLabel: Headline1Label
    lateinit var fiatBalance: Body1Label
    lateinit var unconfirmedBalance: Headline2Label
    lateinit var tokenCount: Headline2Label

    private var walletConfig: WalletConfig? = null

    override fun init(p0: NSCoder?): Long {
        val init = super.init(p0)
        setupView()
        return init
    }

    override fun init(p0: UITableViewCellStyle?, p1: String?): Long {
        val init = super.init(p0, p1)
        setupView()
        return init
    }

    private fun setupView() {
        this.selectionStyle = UITableViewCellSelectionStyle.None
        val cardView = CardView()
        contentView.addSubview(cardView)
        contentView.layoutMargins = UIEdgeInsets.Zero()

        // safe area is bigger than layout here due to margins
        cardView.widthMatchesSuperview(true, 6.0, 6.0, 400.0)
            .superViewWrapsHeight(true, 0.0)

        // init components. This does not work in constructor, init() method is called before constructor
        // (yes - magic of RoboVM)
        nameLabel = Body1BoldLabel()
        balanceLabel = Headline1Label()
        fiatBalance = Body1Label()
        unconfirmedBalance = Headline2Label()
        tokenCount = Headline2Label()

        cardView.contentView.addSubviews(listOf(balanceLabel, nameLabel, fiatBalance, unconfirmedBalance, tokenCount))

        nameLabel.textColor = UIColor.secondaryLabel()
        nameLabel.widthMatchesSuperview().topToSuperview(topInset = 1.0)
        nameLabel.numberOfLines = 1

        balanceLabel.widthMatchesSuperview().topToBottomOf(nameLabel, 1.0)

        fiatBalance.widthMatchesSuperview().topToBottomOf(balanceLabel)
        fiatBalance.textColor = UIColor.secondaryLabel()

        unconfirmedBalance.widthMatchesSuperview().topToBottomOf(fiatBalance)

        tokenCount.widthMatchesSuperview().topToBottomOf(unconfirmedBalance, 5.0)

        tokenCount.bottomToSuperview()
    }

    fun bind(walletData: WalletConfig) {
        walletConfig = walletData
        nameLabel.text = walletData.displayName
        balanceLabel.text = "0.0000 ERG"
        fiatBalance.text = "0.00 EUR"
        unconfirmedBalance.text = "0.0000 ERG unconfirmed"
        unconfirmedBalance.isHidden = true
        tokenCount.text = "0 tokens"
    }

}
