package org.ergoplatform.ios.transactions

import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.isErgoPaySigningRequest
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.STRING_BUTTON_SEND
import org.ergoplatform.uilogic.STRING_DESC_CHOOSE_WALLET
import org.ergoplatform.uilogic.STRING_LABEL_TO
import org.ergoplatform.uilogic.STRING_TITLE_ERGO_PAY_REQUEST
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * Deep link to send funds: Choose wallet to spend from
 */
class ChooseSpendingWalletViewController(
    private val paymentRequest: String,
    val callback: ((Int) -> Unit)
) : CoroutineViewController() {

    private lateinit var walletsStackView: UIStackView

    override fun viewDidLoad() {
        super.viewDidLoad()
        val texts = getAppDelegate().texts

        view.backgroundColor = UIColor.systemBackground()
        addCloseButton()

        val titleLabel = Body1Label().apply {
            textAlignment = NSTextAlignment.Center
        }

        val toLabel = Body1Label().apply {
            text = texts.get(STRING_LABEL_TO)
            textAlignment = NSTextAlignment.Center
        }

        val amountLabel = ErgoAmountView(true, FONT_SIZE_HEADLINE1)

        val recipientLabel = Headline2Label().apply {
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
            textAlignment = NSTextAlignment.Center
        }

        val descLabel = Body1Label().apply {
            text = texts.get(STRING_DESC_CHOOSE_WALLET)
            textAlignment = NSTextAlignment.Center
        }

        walletsStackView = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Vertical
        }
        val scrollView = walletsStackView.wrapInVerticalScrollView()

        view.addSubview(titleLabel)
        view.addSubview(amountLabel)
        view.addSubview(toLabel)
        view.addSubview(recipientLabel)
        view.addSubview(descLabel)
        view.addSubview(scrollView)

        titleLabel.topToSuperview(topInset = DEFAULT_MARGIN * 2)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN)
        amountLabel.topToBottomOf(titleLabel, DEFAULT_MARGIN * 2).centerHorizontal()
        toLabel.topToBottomOf(amountLabel, DEFAULT_MARGIN * 2).centerHorizontal()
        recipientLabel.topToBottomOf(toLabel, DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN * 2)
        descLabel.topToBottomOf(recipientLabel, DEFAULT_MARGIN * 3)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN)
        scrollView.topToBottomOf(descLabel, DEFAULT_MARGIN * 3)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN * 2, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        if (isErgoPaySigningRequest(paymentRequest)) {
            amountLabel.isHidden = true
            recipientLabel.text = ""
            toLabel.text = ""
            titleLabel.text = texts.get(STRING_TITLE_ERGO_PAY_REQUEST)
        } else parsePaymentRequest(paymentRequest)?.let {
            amountLabel.setErgoAmount(it.amount)
            amountLabel.isHidden = it.amount.isZero()
            recipientLabel.text = it.address
            titleLabel.text = texts.get(STRING_BUTTON_SEND)
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            val wallets = getAppDelegate().database.getWalletsWithStates()

            runOnMainThread {
                walletsStackView.clearArrangedSubviews()
                wallets.sortedBy { it.walletConfig.displayName }.forEach { wallet ->
                    walletsStackView.addArrangedSubview(WalletItem(wallet))
                }
            }
        }
    }

    fun navigateToSendFundsScreen(walletId: Int) {
        callback.invoke(walletId)
        dismissViewController(true) {
        }
    }

    private inner class WalletItem(wallet: Wallet) : UIView(CGRect.Zero()) {
        init {
            val name = Body1BoldLabel().apply {
                textColor = uiColorErgo
                numberOfLines = 1
                text = wallet.walletConfig.displayName
            }
            val balance = ErgoAmountView(true).apply {
                setErgoAmount(ErgoAmount(wallet.getBalanceForAllAddresses()))
            }

            addSubview(name)
            addSubview(balance)

            name.topToSuperview().bottomToSuperview().leftToSuperview()
            // lower hugging and compression resistance so that balance will keep its intrinsic
            // width. It does not work using balance.keepIntrinsicWidth here due to
            // ErgoAmountViews inner structure already using it
            name.setContentCompressionResistancePriority(500f, UILayoutConstraintAxis.Horizontal)
            name.setContentHuggingPriority(100f, UILayoutConstraintAxis.Horizontal)
            balance.rightToSuperview().topToSuperview().bottomToSuperview().leftToRightOf(name)

            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                navigateToSendFundsScreen(wallet.walletConfig.id)
            })
        }
    }
}