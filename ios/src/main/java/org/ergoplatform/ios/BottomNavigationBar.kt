package org.ergoplatform.ios

import org.ergoplatform.ios.settings.SettingsViewController
import org.ergoplatform.ios.transactions.ChooseSpendingWalletViewController
import org.ergoplatform.ios.transactions.SendFundsViewController
import org.ergoplatform.ios.ui.IMAGE_SETTINGS
import org.ergoplatform.ios.ui.IMAGE_WALLET
import org.ergoplatform.ios.ui.getAppDelegate
import org.ergoplatform.ios.ui.uiColorErgo
import org.ergoplatform.ios.wallet.WalletViewController
import org.ergoplatform.parsePaymentRequest
import org.ergoplatform.uilogic.STRING_TITLE_SETTINGS
import org.ergoplatform.uilogic.STRING_TITLE_WALLETS
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class BottomNavigationBar : UITabBarController() {

    fun setupVcs() {
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
        tabBar.barTintColor = uiColorErgo
        tabBar.tintColor = UIColor.label()
        tabBar.unselectedItemTintColor = UIColor.label()
        setupVcs()
    }

    fun handlePaymentRequest(paymentRequest: String) {
        val pr = parsePaymentRequest(paymentRequest)

        pr?.let {
            presentViewController(
                ChooseSpendingWalletViewController(pr) { walletId ->
                    navigateToSendFundsScreen(walletId, paymentRequest)
                }, true
            ) {}
        }
    }

    private fun navigateToSendFundsScreen(walletId: Int, paymentRequest: String) {
        // set view to first controller, go back to its root and switch to the wallet's send funds screen
        selectedViewController = viewControllers.first()
        (selectedViewController as? UINavigationController)?.apply {
            popToRootViewController(false)
            pushViewController(
                SendFundsViewController(walletId, paymentRequest = paymentRequest),
                false
            )
        }

    }
}