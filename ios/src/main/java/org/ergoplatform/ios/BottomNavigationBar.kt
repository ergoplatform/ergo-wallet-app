package org.ergoplatform.ios

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ios.ergoauth.ErgoAuthenticationViewController
import org.ergoplatform.ios.settings.SettingsViewController
import org.ergoplatform.ios.transactions.ChooseSpendingWalletViewController
import org.ergoplatform.ios.transactions.ErgoPaySigningViewController
import org.ergoplatform.ios.transactions.SendFundsViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.WalletViewController
import org.ergoplatform.transactions.isErgoPaySigningRequest
import org.ergoplatform.uilogic.MainAppUiLogic
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS
import org.robovm.apple.foundation.Foundation
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSDictionary
import org.robovm.apple.uikit.*

class BottomNavigationBar : UITabBarController() {

    private fun setupVcs() {
        val appDelegate = getAppDelegate()

        setViewControllers(
            NSArray(
                listOf(
                    createNavController(
                        WalletViewController(),
                        appDelegate.texts.get(STRING_TITLE_WALLETS),
                        UIImage.systemImageNamed(IMAGE_WALLET)
                    ),
                    createNavController(
                        SettingsViewController(),
                        appDelegate.texts.get(STRING_TITLE_SETTINGS),
                        UIImage.systemImageNamed(IMAGE_SETTINGS)
                    )
                )
            )
        )
    }

    private fun createNavController(
        rootViewController: UIViewController,
        title: String, image: UIImage
    ): UINavigationController {
        val navController = UINavigationController(rootViewController)
        navController.tabBarItem.title = title
        navController.tabBarItem.image = image
        navController.navigationBar.setPrefersLargeTitles(false)
        val navBarAppearance = UINavigationBarAppearance()
        navBarAppearance.backgroundColor = uiColorErgo
        navController.navigationBar.standardAppearance = navBarAppearance
        navController.navigationBar.scrollEdgeAppearance = navBarAppearance
        rootViewController.navigationItem.title = title
        return navController
    }

    override fun viewDidLoad() {
        if (Foundation.getMajorSystemVersion() >= 15) {
            val appearance = UITabBarAppearance()
            appearance.backgroundColor = uiColorErgo
            setItemAppearance(appearance.compactInlineLayoutAppearance)
            setItemAppearance(appearance.inlineLayoutAppearance)
            setItemAppearance(appearance.stackedLayoutAppearance)
            tabBar.standardAppearance = appearance
            tabBar.scrollEdgeAppearance = appearance
        } else {
            tabBar.barTintColor = uiColorErgo
            tabBar.tintColor = UIColor.label()
            tabBar.unselectedItemTintColor = UIColor.label()
        }
        setupVcs()
    }

    private fun setItemAppearance(itemAppearance: UITabBarItemAppearance) {
        itemAppearance.normal.iconColor = UIColor.label()
        val textAttributes = NSDictionary(
            NSAttributedStringAttribute.Values.ForegroundColor(),
            UIColor.label(),
            NSAttributedStringAttribute.Values.ParagraphStyle(),
            NSParagraphStyle.getDefaultParagraphStyle()
        )
        itemAppearance.normal.titleTextAttributes = textAttributes
        itemAppearance.selected.iconColor = UIColor.label()
        itemAppearance.selected.titleTextAttributes = textAttributes
    }

    fun handlePaymentRequest(paymentRequest: String, fromQr: Boolean) {
        val texts = getAppDelegate().texts
        MainAppUiLogic.handleRequests(paymentRequest,
            fromQr,
            IosStringProvider(texts),
            navigateToChooseWalletDialog = {
                CoroutineScope(Dispatchers.Default).launch {
                    val wallets =
                        getAppDelegate().database.walletDbProvider.getAllWalletConfigsSynchronous()

                    runOnMainThread {
                        if (wallets.size == 1) {
                            navigateToNextScreen(wallets.first().id, paymentRequest, false)
                        } else {
                            presentViewController(
                                ChooseSpendingWalletViewController(paymentRequest) { walletId ->
                                    navigateToNextScreen(walletId, paymentRequest, true)
                                }, true
                            ) {}
                        }
                    }
                }
            },
            navigateToErgoPay = { request ->
                navigateToWalletListAndPushVc(ErgoPaySigningViewController(request), true)
            },
            navigateToAuthentication = { request ->
                navigateToWalletListAndPushVc(ErgoAuthenticationViewController(request, null), true)
            },
            presentUserMessage = { message ->
                presentViewController(buildSimpleAlertController("", message, texts), true) {}
            })
    }

    private fun navigateToWalletListAndPushVc(vc: UIViewController, animated: Boolean) {
        // set view to first controller (wallet list), go back to its root and switch to the
        // wallet's send funds screen
        selectedViewController = viewControllers.first()
        (selectedViewController as? UINavigationController)?.apply {
            popToRootViewController(false)
            pushViewController(vc, animated)
        }
    }

    private fun navigateToNextScreen(
        walletId: Int,
        paymentRequest: String,
        fromChooseScreen: Boolean
    ) {
        navigateToWalletListAndPushVc(
            if (isErgoPaySigningRequest(paymentRequest)) ErgoPaySigningViewController(
                paymentRequest,
                walletId
            )
            else SendFundsViewController(walletId, paymentRequest = paymentRequest),
            !fromChooseScreen
        )
    }
}