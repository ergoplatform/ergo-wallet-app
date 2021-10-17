package org.ergoplatform.ios.ui

import org.ergoplatform.ios.Main
import org.ergoplatform.ios.MyViewController
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class BottomNavigationBar : UITabBarController() {

    fun setupVcs() {
        val appDelegate = UIApplication.getSharedApplication().delegate as Main

        setViewControllers(
            NSArray(
                listOf(
                    createNavController(
                        MyViewController(),
                        appDelegate.texts.get("title_wallets"), UIImage.systemImageNamed("house")
                    ),
                    createNavController(
                        MyViewController(),
                        appDelegate.texts.get("title_settings"), UIImage.systemImageNamed("person")
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