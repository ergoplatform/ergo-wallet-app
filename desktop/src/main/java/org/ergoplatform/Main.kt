package org.ergoplatform

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.ResourceWrapper
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import net.harawata.appdirs.AppDirsFactory
import org.ergoplatform.desktop.Preferences
import org.ergoplatform.desktop.ui.DecomposeDesktopExampleTheme
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.persistance.AppDatabase
import org.ergoplatform.persistance.DbInitializer
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.SqlDelightAppDb
import org.ergoplatform.uilogic.STRING_APP_NAME
import org.ergoplatform.utils.LogUtils
import java.io.File


@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    val root = NavHostComponent(DefaultComponentContext(lifecycle = lifecycle))

    application {
        val windowState = rememberWindowState()
        LifecycleController(lifecycle, windowState)

        // activate for testnet: isErgoMainNet = false
        LogUtils.logDebug = !isErgoMainNet

        Application.texts = I18NBundle.createBundle(ResourceWrapper("/i18n/strings"))

        val appDirs = AppDirsFactory.getInstance()
        val dataDir = appDirs.getUserDataDir("ergowallet", null, null)

        Application.database = SqlDelightAppDb(setupDatabase(dataDir))
        Application.prefs = Preferences.getPrefsFor(dataDir)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = Application.texts.get(STRING_APP_NAME)
        ) {
            DecomposeDesktopExampleTheme {
                root.render()
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
    lateinit var texts: I18NBundle
    lateinit var database: SqlDelightAppDb
    lateinit var prefs: PreferencesProvider
}