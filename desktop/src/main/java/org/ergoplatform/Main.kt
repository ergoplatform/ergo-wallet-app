package org.ergoplatform

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.decompose.router.navigate
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.ResourceWrapper
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.github.sanyarnd.applocker.AppLocker
import net.harawata.appdirs.AppDirsFactory
import org.ergoplatform.desktop.Preferences
import org.ergoplatform.desktop.persistance.DesktopCacheFileManager
import org.ergoplatform.desktop.ui.DecomposeDesktopExampleTheme
import org.ergoplatform.desktop.ui.DesktopStringProvider
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.desktop.wallet.WalletListComponent
import org.ergoplatform.mosaik.MosaikComposeConfig
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.persistance.*
import org.ergoplatform.uilogic.STRING_APP_NAME
import org.ergoplatform.utils.LogUtils
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess


@OptIn(ExperimentalDecomposeApi::class)
fun main(args: Array<String>) {
    val lifecycle = LifecycleRegistry()
    val root = NavHostComponent(DefaultComponentContext(lifecycle = lifecycle))
    val bringToTop = mutableStateOf(false)

    Application.texts =
        DesktopStringProvider(I18NBundle.createBundle(ResourceWrapper("/i18n/strings")))

    val appDirs = AppDirsFactory.getInstance()
    val cacheDir = appDirs.getUserCacheDir("ergowallet", null, null)
    val dataDir = appDirs.getUserDataDir("ergowallet", null, null)

    Application.filesCache = DesktopCacheFileManager(cacheDir)

    // Process CLI arguments and check for existing appliation instance
    if (args.any { it.equals("--testnet", true) }) {
        isErgoMainNet = false
    }
    if (args.any { it.equals("--debug", true) }) {
        LogUtils.logDebug = true
    }
    Application.startUpArguments = args.toList()
    val locker = AppLocker.create("org.ergoplatform.ergowallet.app#$isErgoMainNet")
        .setPath(Paths.get(cacheDir))
        .setMessageHandler { message ->
            bringToTop.value = true
            LogUtils.logDebug("AppLocker", "Received instance message $message")

            if (message.toString().isNotBlank()) {
                Application.startUpArguments = message.toString().split(' ')

                val currentComponent = root.router.state.value.activeChild.instance
                if (currentComponent is WalletListComponent){
                    currentComponent.processStartUpArguments()
                } else {
                    // FIXME this only resets the navigation, but the selected bottom nav item does not change
                    root.router.navigate { mutableListOf(ScreenConfig.WalletList) }
                }
            }

            ""
        } // handle messages (default: NULL)
        .onBusy(
            Application.startUpArguments!!.joinToString(" ")
        ) { exitProcess(0) } // send message to the instance which currently owns the lock and exit
        .build()
    locker.lock()

    // Database and prefs init
    Application.database = SqlDelightAppDb(setupDatabase(dataDir))
    val desktopPrefs = Preferences.getPrefsFor(dataDir)
    Application.prefs = desktopPrefs

    WalletStateSyncManager.getInstance()
        .loadPreferenceValues(Application.prefs, Application.database)

    val windowSize = desktopPrefs.windowSize
    val windowPos = desktopPrefs.windowPos

    // set Mosaik and app style
    MosaikComposeConfig.scrollMinAlpha = 1f
    MosaikStyleConfig.apply {
        primaryLabelColor = uiErgoColor
        secondaryButtonColor = Color.White.copy(alpha = 0.87f) // 0.87 is LocalContentAlpha.current
        secondaryButtonTextColor = Color.Black
        textButtonTextColor = uiErgoColor
        defaultLabelColor = secondaryButtonColor
        secondaryLabelColor = Color(0xFFBBBBBB)
    }

    application {
        val windowState = rememberWindowState(
            position = if (windowPos.second < 0f) WindowPosition(Alignment.Center) else WindowPosition(
                windowPos.first.dp,
                windowPos.second.dp
            ),
            size = DpSize(max(300.dp, windowSize.first.dp), max(200.dp, windowSize.second.dp))
        )
        LifecycleController(lifecycle, windowState)
        val windowIcon = painterResource("icon.png")

        Window(
            onCloseRequest = {
                desktopPrefs.windowSize =
                    Pair(windowState.size.width.value, windowState.size.height.value)
                desktopPrefs.windowPos =
                    Pair(windowState.position.x.value, windowState.position.y.value)
                locker.unlock()
                exitApplication()
            },
            state = windowState,
            icon = windowIcon,
            title = Application.texts.getString(STRING_APP_NAME)
        ) {
            LaunchedEffect(bringToTop.value) {
                if (bringToTop.value) {
                    // workarounds to really bring the window in front
                    window.isVisible = false
                    window.isVisible = true
                    window.toFront()
                    window.isMinimized = false
                    bringToTop.value = false
                }
            }

            DecomposeDesktopExampleTheme {
                root.render(null)
            }
        }
    }
}

private fun setupDatabase(dataDir: String): AppDatabase {
    val dbname = if (isErgoMainNet) "wallet" else "wallet_test"

    val libraryPath = File(dataDir)
    libraryPath.mkdirs()
    val dbFileName = File(libraryPath, "$dbname.db").absolutePath
    LogUtils.logDebug("Database", "Open db at $dbFileName")

    val driver = JdbcSqliteDriver("jdbc:sqlite:$dbFileName")

    DbInitializer.initDbSchema(driver)

    return AppDatabase(driver)
}

object Application {
    lateinit var texts: DesktopStringProvider
    lateinit var database: SqlDelightAppDb
    lateinit var prefs: PreferencesProvider
    lateinit var filesCache: CacheFileManager

    var startUpArguments: List<String>? = null
}