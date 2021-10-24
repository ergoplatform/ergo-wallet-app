package org.ergoplatform.ios

import SQLite.JDBCDriver
import com.badlogic.gdx.utils.I18NBundle
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.ergoplatform.ios.ui.ViewControllerWithKeyboardLayoutGuide
import org.ergoplatform.persistance.AppDatabase
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.foundation.NSBundle
import org.robovm.apple.uikit.*
import java.io.File
import java.sql.DriverManager

class Main : UIApplicationDelegateAdapter() {
    lateinit var database: AppDatabase
    lateinit var texts: I18NBundle

    private val keyboardObservers = ArrayList<ViewControllerWithKeyboardLayoutGuide>()

    // xibless no story board approach
    // see https://github.com/lanqy/swift-programmatically

    override fun didFinishLaunching(
        application: UIApplication,
        launchOptions: UIApplicationLaunchOptions?
    ): Boolean {
        val internalPath = NSBundle.getMainBundle().bundlePath

        database = setupDatabase("wallet.db")
        texts = I18NBundle.createBundle(File(internalPath, "i18n/strings"))

        // Set up the view controller.
        val rootViewController = BottomNavigationBar()

        // Create a new window at screen size.
        window = UIWindow(UIScreen.getMainScreen().bounds)
        // Set the view controller as the root controller for the window.
        window.rootViewController = rootViewController
        // Make the window visible.
        window.makeKeyAndVisible()

        startKeyboardObserver()
        return true
    }

    private fun startKeyboardObserver() {
        UIWindow.Notifications.observeKeyboardWillShow { keyboard ->
            val duration = keyboard.animationDuration
            keyboardObservers.forEach { it.adjustKeyboardHeight(keyboard.endFrame, duration) }

        }
        UIWindow.Notifications.observeKeyboardWillChangeFrame { keyboard ->
            val duration = keyboard.animationDuration
            keyboardObservers.forEach { it.adjustKeyboardHeight(keyboard.endFrame, duration) }

        }
        UIWindow.Notifications.observeKeyboardWillHide { keyboard ->
            keyboardObservers.forEach { it.adjustKeyboardHeight(null, keyboard.animationDuration) }
        }
    }

    fun addKeyboardObserver(vc: ViewControllerWithKeyboardLayoutGuide) {
        keyboardObservers.add(vc)
    }

    fun removeKeyboardObserver(vc: ViewControllerWithKeyboardLayoutGuide) {
        keyboardObservers.remove(vc)
    }

    private fun setupDatabase(dbname: String): AppDatabase {
        // retrieve directory
        val dbPath = File(System.getenv("HOME"), "Library/").absolutePath

        // register RoboVMs Sqlite driver
        DriverManager.registerDriver(JDBCDriver())

        val driver = JdbcSqliteDriver("sqlite:/" + dbPath + dbname)

        AppDatabase.Schema.create(driver)

        return AppDatabase(driver)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            NSAutoreleasePool().use { _ ->
                UIApplication.main(
                    args,
                    null as? Class<UIApplication>,
                    Main::class.java
                )
            }
        }
    }
}