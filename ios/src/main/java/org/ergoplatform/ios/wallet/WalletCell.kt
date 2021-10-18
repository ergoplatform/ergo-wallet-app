package org.ergoplatform.ios.wallet

import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletConfig
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.*

const val WALLET_CELL = "WalletCell"

class WalletCell : UITableViewCell(UITableViewCellStyle.Default, WALLET_CELL) {
    lateinit var nameLabel: Body1Label
    lateinit var balanceLabel: Headline1Label

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
        nameLabel = Body1BoldLabel()
        balanceLabel = Headline1Label()

        this.selectionStyle = UITableViewCellSelectionStyle.None
        val cardView = CardView()
        contentView.addSubview(cardView)
        contentView.layoutMargins = UIEdgeInsets.Zero()
        cardView.contentView.apply {
            addSubview(balanceLabel)
            addSubview(nameLabel)
        }

        // safe area is bigger than layout here due to margins
        cardView.widthMatchesSuperview(true, 6.0, 6.0, 400.0)
            .superViewWrapsHeight(true, 0.0)

        nameLabel.widthMatchesSuperview().topToSuperview(topInset = 1.0)
        nameLabel.numberOfLines = 1

        balanceLabel.widthMatchesSuperview().bottomToSuperview().topToBottomOf(nameLabel, 1.0)

        //nameLabel.textAlignment = NSTextAlignment.Center

    }

    fun bind(walletData: WalletConfig) {
        nameLabel.text = walletData.displayName
        balanceLabel.text = "0.0000 ERG"
    }

}
