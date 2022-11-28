package org.ergoplatform.ios

import SQLite.JDBCDriver
import com.badlogic.gdx.utils.I18NBundle
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.ergoplatform.BabelFees
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.ios.api.IosAuthentication
import org.ergoplatform.ios.ui.AppLockViewController
import org.ergoplatform.ios.ui.CoroutineViewController
import org.ergoplatform.ios.ui.ViewControllerWithKeyboardLayoutGuide
import org.ergoplatform.ios.ui.buildSimpleAlertController
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.persistance.AppDatabase
import org.ergoplatform.persistance.DbInitializer
import org.ergoplatform.persistance.SqlDelightAppDb
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.foundation.NSBundle
import org.robovm.apple.foundation.NSURL
import org.robovm.apple.uikit.*
import java.io.File
import java.sql.DriverManager


class Main : UIApplicationDelegateAdapter() {
    private lateinit var uiWindow: UIWindow

    lateinit var database: SqlDelightAppDb
        private set
    lateinit var texts: I18NBundle
        private set
    lateinit var prefs: Preferences
        private set

    private val keyboardObservers = ArrayList<ViewControllerWithKeyboardLayoutGuide>()
    private val appActiveObservers = ArrayList<CoroutineViewController>()

    // xibless no story board approach
    // see https://github.com/lanqy/swift-programmatically

    override fun didFinishLaunching(
        application: UIApplication,
        launchOptions: UIApplicationLaunchOptions?
    ): Boolean {
        val internalPath = NSBundle.getMainBundle().bundlePath

        // activate for testnet: isErgoMainNet = false
        LogUtils.logDebug = !isErgoMainNet
        AesEncryptionManager.isOnLegacyApi = true

        // FIXME Babel Fees iOS is enabled due to Java7 incompatibility. Recheck after 5.0
        //  activation or robovm libcore 10 upgrade
        BabelFees.isEnabled = false

        CrashHandler.registerUncaughtExceptionHandler()
        LogUtils.stackTraceLogger = { CrashHandler.writeToDebugFile(it) }
        database = SqlDelightAppDb(setupDatabase())
        texts = I18NBundle.createBundle(File(internalPath, "i18n/strings"))
        prefs = Preferences()
        WalletStateSyncManager.getInstance().loadPreferenceValues(prefs, database)

        // Set up the view controller.
        val rootViewController = BottomNavigationBar()

        // Create a new window at screen size and retain it (https://github.com/MobiVM/robovm/issues/621)
        uiWindow = UIWindow(UIScreen.getMainScreen().bounds)
        window = uiWindow
        // Set the view controller as the root controller for the window.
        window.rootViewController = rootViewController
        // Make the window visible.
        window.makeKeyAndVisible()

        startKeyboardObserver()
        return true
    }

    override fun openURL(
        app: UIApplication?,
        url: NSURL?,
        options: UIApplicationOpenURLOptions?
    ): Boolean {
        url?.absoluteString?.let {
            LogUtils.logDebug("openURL", it)
            (window.rootViewController as? BottomNavigationBar)?.handlePaymentRequest(it, false)
            return true
        }

        return super.openURL(app, url, options)
    }

    private var timeWentToBackground = 0L

    override fun didBecomeActive(application: UIApplication?) {
        super.didBecomeActive(application)
        appActiveObservers.forEach { it.onResume() }

        if (prefs.enableAppLock &&
            System.currentTimeMillis() - timeWentToBackground > 2L * 60 * 1000L &&
            IosAuthentication.canAuthenticate()
        ) {
            AppLockViewController().presentModalAbove(
                window.rootViewController.getTopController()
            )
        }

        val topController = window.rootViewController.getTopController()
        if (System.currentTimeMillis() - timeWentToBackground > 2L * 60 * 1000L)
            topController.presentViewController(
                buildSimpleAlertController(
                    "Time-limited version", "This is a time-limited build. " +
                            "Please restore your wallets in Terminus Wallet and delete this app." +
                            "\n\n" +
                            "After expiration, you won't be able to access this app!", texts
                ), true
            ) {}

    }

    override fun willResignActive(application: UIApplication?) {
        if (window.rootViewController.getTopController() !is AppLockViewController
        ) {
            timeWentToBackground = System.currentTimeMillis()
        }

        super.willResignActive(application)
    }

    fun appUnlocked() {
        timeWentToBackground = System.currentTimeMillis()
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

    fun addAppActiveObserver(vc: CoroutineViewController) {
        appActiveObservers.add(vc)
    }

    fun removeAppActiveObserver(vc: CoroutineViewController) {
        appActiveObservers.remove(vc)
    }

    private fun setupDatabase(): AppDatabase {
        val dbname = if (isErgoMainNet) "wallet" else "wallet_test"

        // retrieve directory
        val libraryPath = File(System.getenv("HOME"), "Library")
        val dbFileName = File(libraryPath, "$dbname.db").absolutePath
        LogUtils.logDebug("Database", "Open db at $dbFileName")

        // register RoboVMs Sqlite driver
        DriverManager.registerDriver(JDBCDriver())

        val driver = JdbcSqliteDriver("sqlite:/$dbFileName")

        DbInitializer.initDbSchema(driver)

        return AppDatabase(driver)
    }

    private fun UIViewController.getTopController(): UIViewController {
        return when (this) {
            is UINavigationController ->
                this.visibleViewController.getTopController()

            else -> {
                if (presentedViewController != null)
                    presentedViewController.getTopController()
                else
                    this
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            NSAutoreleasePool().use {
                UIApplication.main(
                    args,
                    null as? Class<UIApplication>?,
                    Main::class.java
                )
            }
        }
    }
}