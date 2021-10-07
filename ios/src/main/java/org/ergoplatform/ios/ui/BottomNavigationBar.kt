package org.ergoplatform.ios.ui

import org.ergoplatform.ios.MyViewController
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class BottomNavigationBar : UITabBarController() {

    fun setupVcs() {
        // TODO load from i18n file
        setViewControllers(
            NSArray(
                listOf(
                    createNavController(
                        MyViewController(),
                        "Wallet", UIImage.systemImageNamed("house")
                    ),
                    createNavController(
                        MyViewController(),
                        "Settings", UIImage.systemImageNamed("person")
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
        //navController.navigationBar.setPrefersLargeTitles(true)
        rootViewController.navigationItem.title = title
        return navController
    }

    override fun viewDidLoad() {
        tabBar.barTintColor = UIColor.systemBackground()
        tabBar.tintColor = UIColor.label()
        setupVcs()
    }
}