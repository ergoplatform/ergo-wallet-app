package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ErgoAmount
import org.ergoplatform.NodeConnector
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_BUTTON_RECEIVE
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.uilogic.STRING_LABEL_UNCONFIRMED
import org.ergoplatform.uilogic.STRING_TITLE_TRANSACTIONS
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses
import org.ergoplatform.wallet.getUnconfirmedBalanceForAllAddresses
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSCoder
import org.robovm.apple.uikit.*

const val WALLET_CELL = "EmptyCell"
const val EMPTY_CELL = "WalletCell"

class WalletCell : UITableViewCell(UITableViewCellStyle.Default, WALLET_CELL) {
    private lateinit var nameLabel: Body1Label
    private lateinit var balanceLabel: Headline1Label
    private lateinit var fiatBalance: Body1Label
    private lateinit var unconfirmedBalance: Headline2Label
    private lateinit var tokenCount: Headline2Label
    private lateinit var transactionButton: UIButton
    private lateinit var receiveButton: UIButton
    private lateinit var sendButton: UIButton
    private lateinit var textBundle: I18NBundle

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
        cardView.widthMatchesSuperview(true, DEFAULT_MARGIN, MAX_WIDTH)
            .superViewWrapsHeight(true, 0.0)

        // init components. This does not work in constructor, init() method is called before constructor
        // (yes - magic of RoboVM)
        nameLabel = Body1BoldLabel()
        balanceLabel = Headline1Label()
        fiatBalance = Body1Label()
        unconfirmedBalance = Headline2Label()
        val spacing = UIView(CGRect.Zero())
        tokenCount = Headline2Label()

        textBundle = getAppDelegate().texts
        transactionButton = CommonButton(textBundle.get(STRING_TITLE_TRANSACTIONS))
        receiveButton = CommonButton(textBundle.get(STRING_BUTTON_RECEIVE))
        sendButton = PrimaryButton(textBundle.get(STRING_BUTTON_SEND))

        val stackView =
            UIStackView(
                NSArray(
                    nameLabel,
                    balanceLabel,
                    fiatBalance,
                    unconfirmedBalance,
                    spacing,
                    tokenCount
                )
            )
        stackView.alignment = UIStackViewAlignment.Leading
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.setCustomSpacing(DEFAULT_MARGIN * 1.5, spacing)

        val walletImage = UIImageView(
            UIImage.getSystemImage(
                IMAGE_WALLET,
                UIImageSymbolConfiguration.getConfigurationWithPointSizeWeightScale(
                    30.0,
                    UIImageSymbolWeight.Regular,
                    UIImageSymbolScale.Large
                )
            )
        )
        walletImage.tintColor = UIColor.secondaryLabel()

        val horizontalStack = UIStackView(NSArray(receiveButton, sendButton))
        horizontalStack.spacing = DEFAULT_MARGIN
        horizontalStack.distribution = UIStackViewDistribution.FillEqually

        cardView.contentView.addSubviews(
            listOf(
                stackView,
                walletImage,
                transactionButton,
                horizontalStack
            )
        )
        walletImage.topToSuperview(false, DEFAULT_MARGIN * 2)
            .leftToSuperview(false, DEFAULT_MARGIN)
        stackView.leftToRightOf(walletImage, DEFAULT_MARGIN).topToSuperview()
            .rightToSuperview(false, DEFAULT_MARGIN)

        nameLabel.textColor = UIColor.secondaryLabel()
        nameLabel.numberOfLines = 1

        fiatBalance.textColor = UIColor.secondaryLabel()

        transactionButton.widthMatchesSuperview(false, DEFAULT_MARGIN)
            .topToBottomOf(stackView, DEFAULT_MARGIN * 3)

        horizontalStack.widthMatchesSuperview(false, DEFAULT_MARGIN)
            .topToBottomOf(transactionButton, DEFAULT_MARGIN)
            .bottomToSuperview(false, DEFAULT_MARGIN)

    }

    fun bind(wallet: Wallet) {
        walletConfig = wallet.walletConfig
        nameLabel.text = wallet.walletConfig.displayName
        val ergoAmount = ErgoAmount(wallet.getBalanceForAllAddresses())
        balanceLabel.text = ergoAmount.toString() + " ERG"
        val nodeConnector = NodeConnector.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value
        fiatBalance.isHidden = ergoPrice == 0f
        fiatBalance.text = String.format("%.2f", ergoPrice.toDouble() * ergoAmount.toDouble()) + " " +
                nodeConnector.fiatCurrency.uppercase()
        val unconfirmedErgs = wallet.getUnconfirmedBalanceForAllAddresses()
        unconfirmedBalance.text = ErgoAmount(unconfirmedErgs).toString() + " ERG " +
                textBundle.get(STRING_LABEL_UNCONFIRMED)
        unconfirmedBalance.isHidden = unconfirmedErgs == 0L
        val tokens = wallet.getTokensForAllAddresses()
        tokenCount.text = tokens.size.toString() + " tokens"
    }

}

class EmptyCell : UITableViewCell(UITableViewCellStyle.Default, WALLET_CELL) {
    lateinit var walletChooserStackView: AddWalletChooserStackView

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
        walletChooserStackView = AddWalletChooserStackView(getAppDelegate().texts)
        contentView.addSubview(walletChooserStackView)
        walletChooserStackView.edgesToSuperview(false, DEFAULT_MARGIN, MAX_WIDTH)
    }
}
