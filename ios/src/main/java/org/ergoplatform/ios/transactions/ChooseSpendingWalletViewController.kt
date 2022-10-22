package org.ergoplatform.ios.transactions

import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ios.ui.*
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.*
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses
import org.ergoplatform.wallet.sortedByDisplayName
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
    private lateinit var texts: StringProvider

    override fun viewDidLoad() {
        super.viewDidLoad()
        texts = IosStringProvider(getAppDelegate().texts)

        view.backgroundColor = UIColor.systemBackground()
        addCloseButton()

        val titleLabel = Body1Label().apply {
            textAlignment = NSTextAlignment.Center
        }

        val toLabel = Body1Label().apply {
            text = texts.getString(STRING_LABEL_TO)
            textAlignment = NSTextAlignment.Center
        }

        val amountLabel = ErgoAmountView(true, FONT_SIZE_HEADLINE1)

        val recipientLabel = Headline2Label().apply {
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
            textAlignment = NSTextAlignment.Center
        }

        val descLabel = Body1Label().apply {
            text = texts.getString(STRING_DESC_CHOOSE_WALLET)
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

        parsePaymentRequest(paymentRequest)?.let {
            amountLabel.setErgoAmount(it.amount)
            amountLabel.isHidden = it.amount.isZero()
            recipientLabel.text = it.address
            titleLabel.text = texts.getString(STRING_BUTTON_SEND)
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            val wallets = getAppDelegate().database.walletDbProvider.getWalletsWithStates()

            runOnMainThread {
                walletsStackView.clearArrangedSubviews()
                wallets.sortedByDisplayName().forEach { wallet ->
                    walletsStackView.addArrangedSubview(ChooseWalletItem(wallet, texts).apply {
                        isUserInteractionEnabled = true
                        addGestureRecognizer(UITapGestureRecognizer {
                            navigateToSendFundsScreen(wallet.walletConfig.id)
                        })
                    })
                }
            }
        }
    }

    private fun navigateToSendFundsScreen(walletId: Int) {
        callback.invoke(walletId)
        dismissViewController(true) {}
    }

    class ChooseWalletItem(
        wallet: Wallet,
        texts: StringProvider,
        showTokenNum: Boolean = false
    ) : UIView(CGRect.Zero()) {
        init {
            val name = Body1BoldLabel().apply {
                textColor = uiColorErgo
                numberOfLines = 1
                text = wallet.walletConfig.displayName
            }
            val balance = ErgoAmountView(true).apply {
                setErgoAmount(ErgoAmount(wallet.getBalanceForAllAddresses()))
            }
            val tokenNumLabel = Body1BoldLabel()
            val tokenNum = if (!showTokenNum) 0 else wallet.getTokensForAllAddresses().size
            tokenNumLabel.text = if (tokenNum > 0)
                texts.getString(STRING_LABEL_WALLET_TOKEN_BALANCE, tokenNum) else ""

            addSubview(name)
            addSubview(balance)
            addSubview(tokenNumLabel)

            name.topToSuperview().bottomToSuperview().leftToSuperview()
            // lower hugging and compression resistance so that balance will keep its intrinsic
            // width. It does not work using balance.keepIntrinsicWidth here due to
            // ErgoAmountViews inner structure already using it
            name.setContentCompressionResistancePriority(500f, UILayoutConstraintAxis.Horizontal)
            name.setContentHuggingPriority(100f, UILayoutConstraintAxis.Horizontal)
            balance.rightToSuperview().topToSuperview().leftToRightOf(name)
            tokenNumLabel.rightToRightOf(balance).topToBottomOf(balance).bottomToSuperview()
        }
    }
}