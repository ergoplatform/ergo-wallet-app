package org.ergoplatform.ios

import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.WalletViewController
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
                        MyViewController(),
                        appDelegate.texts.get(STRING_TITLE_WALLETS),
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
        navController.navigationBar.setPrefersLargeTitles(true)
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
        tabBar.unselectedItemTintColor = UIColor.secondaryLabel()
        setupVcs()
    }
}