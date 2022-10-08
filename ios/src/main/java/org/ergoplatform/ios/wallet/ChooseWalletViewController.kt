package org.ergoplatform.ios.wallet

import kotlinx.coroutines.launch
import org.ergoplatform.ios.transactions.ChooseSpendingWalletViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_ERROR_NO_WALLET
import org.ergoplatform.uilogic.StringProvider
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ChooseWalletViewController(
    val callback: ((WalletConfig) -> Unit)
) : CoroutineViewController() {
    private lateinit var walletsStackView: UIStackView
    private lateinit var texts: StringProvider

    override fun viewDidLoad() {
        super.viewDidLoad()
        texts = IosStringProvider(getAppDelegate().texts)

        view.backgroundColor = UIColor.systemBackground()
        val closeButton = addCloseButton()

        walletsStackView = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Vertical
        }
        val scrollView = walletsStackView.wrapInVerticalScrollView()

        view.addSubview(scrollView)

        scrollView.topToBottomOf(closeButton, DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN * 2, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            val wallets = getAppDelegate().database.walletDbProvider.getWalletsWithStates()

            runOnMainThread {
                walletsStackView.clearArrangedSubviews()
                wallets.sortedBy { it.walletConfig.displayName?.lowercase() }.forEach { wallet ->
                    walletsStackView.addArrangedSubview(
                        ChooseSpendingWalletViewController.ChooseWalletItem(
                            wallet,
                            texts,
                            true
                        ).apply {
                            isUserInteractionEnabled = true
                            addGestureRecognizer(UITapGestureRecognizer {
                                callback.invoke(wallet.walletConfig)
                                dismissViewController(true) {}
                            })
                        })
                }

                if (wallets.isEmpty()) {
                    val errorLabel = Body1Label().apply {
                        text = texts.getString(STRING_ERROR_NO_WALLET)
                        textAlignment = NSTextAlignment.Center
                    }
                    view.addSubview(errorLabel)
                    errorLabel.widthMatchesSuperview(inset = DEFAULT_MARGIN * 3, maxWidth = MAX_WIDTH).centerVertical()
                }
            }
        }
    }

}