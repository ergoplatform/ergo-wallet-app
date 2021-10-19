package org.ergoplatform.ios

import SQLite.JDBCDriver
import com.badlogic.gdx.utils.I18NBundle
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.ergoplatform.persistance.AppDatabase
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.foundation.NSBundle
import org.robovm.apple.uikit.*
import java.io.File
import java.sql.DriverManager

class Main : UIApplicationDelegateAdapter() {
    lateinit var database: AppDatabase
    lateinit var texts: I18NBundle

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
        return true
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