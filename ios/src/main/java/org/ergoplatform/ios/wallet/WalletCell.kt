package org.ergoplatform.ios.wallet

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ios.tokens.TokenEntryView
import org.ergoplatform.ios.transactions.ReceiveToWalletViewController
import org.ergoplatform.ios.transactions.SendFundsViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.tokens.fillTokenOverview
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses
import org.ergoplatform.wallet.getUnconfirmedBalanceForAllAddresses
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

const val WALLET_CELL = "EmptyCell"
const val EMPTY_CELL = "WalletCell"

class WalletCell : AbstractTableViewCell(WALLET_CELL) {
    var clickListener: ((vc: UIViewController) -> Unit)? = null

    private lateinit var nameLabel: Body1Label
    private lateinit var balanceLabel: ErgoAmountView
    private lateinit var fiatBalance: Body1Label
    private lateinit var unconfirmedBalance: Body1BoldLabel
    private lateinit var tokenCount: Headline2Label
    private lateinit var unfoldTokensButton: UIImageView
    private lateinit var detailsButton: UIButton
    private lateinit var receiveButton: UIButton
    private lateinit var sendButton: UIButton
    private lateinit var textBundle: I18NBundle
    private lateinit var tokenStack: UIStackView
    private lateinit var configButton: UIView

    private var wallet: Wallet? = null

    override fun setupView() {
        this.selectionStyle = UITableViewCellSelectionStyle.None
        val cardView = CardView()
        contentView.addSubview(cardView)
        contentView.layoutMargins = UIEdgeInsets.Zero()

        // safe area is bigger than layout here due to margins
        cardView.widthMatchesSuperview(true, DEFAULT_MARGIN, MAX_WIDTH)
            .superViewWrapsHeight(true, 0.0)

        textBundle = getAppDelegate().texts

        // init components. This does not work in constructor, init() method is called before constructor
        // (yes - magic of RoboVM)
        nameLabel = Body1BoldLabel()
        balanceLabel = ErgoAmountView(true, FONT_SIZE_HEADLINE1)
        fiatBalance = Body1Label()

        unconfirmedBalance = Body1BoldLabel().apply {
            numberOfLines = 1
        }

        val spacing = UIView(CGRect.Zero())
        tokenCount = Headline2Label()
        tokenStack = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Vertical
            setSpacing(DEFAULT_MARGIN / 3)
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN * .5, DEFAULT_MARGIN * 2, 0.0, 0.0)
            isLayoutMarginsRelativeArrangement = true
        }
        configButton = UIImageView(getIosSystemImage(IMAGE_SETTINGS, UIImageSymbolScale.Small)).apply {
            tintColor = UIColor.label()
        }

        detailsButton = CommonButton(textBundle.get(STRING_LABEL_DETAILS))
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
                    tokenCount,
                    tokenStack
                )
            )
        stackView.alignment = UIStackViewAlignment.Leading
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.setCustomSpacing(DEFAULT_MARGIN * 1.5, spacing)

        val walletImage = UIImageView(getIosSystemImage(IMAGE_WALLET, UIImageSymbolScale.Large))
        walletImage.tintColor = UIColor.secondaryLabel()
        walletImage.enforceKeepIntrinsicWidth()

        val transactionButtonStack = UIStackView(NSArray(receiveButton, sendButton))
        transactionButtonStack.spacing = DEFAULT_MARGIN
        transactionButtonStack.distribution = UIStackViewDistribution.FillEqually

        unfoldTokensButton = UIImageView()
        unfoldTokensButton.tintColor = UIColor.label()

        cardView.contentView.addSubviews(
            listOf(
                stackView,
                walletImage,
                detailsButton,
                transactionButtonStack,
                unfoldTokensButton,
                configButton
            )
        )
        walletImage.topToSuperview(false, DEFAULT_MARGIN * 2)
            .leftToSuperview(false, DEFAULT_MARGIN)
        stackView.leftToRightOf(walletImage, DEFAULT_MARGIN).topToSuperview()
            .rightToSuperview(false, DEFAULT_MARGIN)

        nameLabel.textColor = UIColor.secondaryLabel()
        nameLabel.numberOfLines = 1

        fiatBalance.textColor = UIColor.secondaryLabel()

        detailsButton.widthMatchesSuperview(false, DEFAULT_MARGIN)
            .topToBottomOf(stackView, DEFAULT_MARGIN * 3)

        transactionButtonStack.widthMatchesSuperview(false, DEFAULT_MARGIN)
            .topToBottomOf(detailsButton, DEFAULT_MARGIN)
            .bottomToSuperview(false, DEFAULT_MARGIN)

        configButton.topToSuperview().rightToSuperview()

        unfoldTokensButton.centerVerticallyTo(tokenCount)
        unfoldTokensButton.rightToLeftOf(tokenCount, DEFAULT_MARGIN)

        cardView.contentView.isUserInteractionEnabled = true
        cardView.contentView.addGestureRecognizer(UITapGestureRecognizer {
            detailsButtonClicked()
        })
        detailsButton.addOnTouchUpInsideListener { _, _ -> detailsButtonClicked() }

        receiveButton.addOnTouchUpInsideListener { _, _ ->
            receiveButtonClicked()
        }

        sendButton.addOnTouchUpInsideListener { _, _ -> sendButtonClicked() }

        configButton.isUserInteractionEnabled = true
        configButton.addGestureRecognizer(UITapGestureRecognizer {
            configButtonClicked()
        })

        unfoldTokensButton.isUserInteractionEnabled = true
        unfoldTokensButton.addGestureRecognizer(UITapGestureRecognizer { toggleTokenUnfold() })
        tokenCount.isUserInteractionEnabled = true
        tokenCount.addGestureRecognizer(UITapGestureRecognizer { toggleTokenUnfold() })
    }

    private fun toggleTokenUnfold() {
        wallet?.walletConfig?.let { config ->
            getAppDelegate().database.walletDbProvider.updateWalletDisplayTokens(!config.unfoldTokens, config.id)
        }
    }

    fun bind(wallet: Wallet) {
        if (!wallet.walletConfig.firstAddress.equals(this.wallet?.walletConfig?.firstAddress)) {
            bindImmediately(wallet)
        } else {
            contentView.layer.removeAllAnimations()
            contentView.animateLayoutChanges { bindImmediately(wallet) }
        }
    }

    private fun bindImmediately(wallet: Wallet) {
        this.wallet = wallet
        nameLabel.text = wallet.walletConfig.displayName
        val ergoAmount = ErgoAmount(wallet.getBalanceForAllAddresses())
        balanceLabel.setErgoAmount(ergoAmount)
        val nodeConnector = WalletStateSyncManager.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value
        fiatBalance.isHidden = ergoPrice == 0f
        fiatBalance.text = formatFiatToString(
            ergoPrice.toDouble() * ergoAmount.toDouble(),
            nodeConnector.fiatCurrency, IosStringProvider(textBundle)
        )
        val unconfirmedErgs = wallet.getUnconfirmedBalanceForAllAddresses()
        unconfirmedBalance.text = if (unconfirmedErgs == 0L) "" else
            textBundle.format(
                STRING_LABEL_ERG_AMOUNT,
                ErgoAmount(unconfirmedErgs).toStringRoundToDecimals()
            ) + " " + textBundle.get(STRING_LABEL_UNCONFIRMED)
        val tokens = wallet.getTokensForAllAddresses()
        tokenCount.text = tokens.size.toString() + " tokens"
        tokenCount.isHidden = tokens.isEmpty()
        unfoldTokensButton.isHidden = tokens.isEmpty()
        unfoldTokensButton.image = getIosSystemImage(
            if (!wallet.walletConfig.unfoldTokens) IMAGE_PLUS_CIRCLE else IMAGE_MINUS_CIRCLE,
            UIImageSymbolScale.Small
        )

        tokenStack.clearArrangedSubviews()
        if (wallet.walletConfig.unfoldTokens) {
            fillTokenOverview(tokens, {
                tokenStack.addArrangedSubview(TokenEntryView().bindWalletToken(it, textBundle))
            }, {
                tokenStack.addArrangedSubview(TokenEntryView().bindHasMoreTokenHint(it))
            })
        }
    }

    private fun detailsButtonClicked() {
        clickListener?.invoke(WalletDetailsViewController(wallet!!.walletConfig.id))
    }

    private fun receiveButtonClicked() {
        LogUtils.logDebug("WalletCell", "Clicked receive")
        clickListener?.invoke(ReceiveToWalletViewController(wallet!!.walletConfig.id))
    }

    private fun sendButtonClicked() {
        clickListener?.invoke(SendFundsViewController(wallet!!.walletConfig.id))
    }

    private fun configButtonClicked() {
        clickListener?.invoke(WalletConfigViewController(wallet!!.walletConfig.id))
    }
}

class EmptyCell : AbstractTableViewCell(EMPTY_CELL) {
    lateinit var walletChooserStackView: AddWalletChooserStackView

    override fun setupView() {
        this.selectionStyle = UITableViewCellSelectionStyle.None
        walletChooserStackView = AddWalletChooserStackView(getAppDelegate().texts)
        contentView.addSubview(walletChooserStackView)
        walletChooserStackView.edgesToSuperview(false, DEFAULT_MARGIN, MAX_WIDTH)
    }
}
