import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.ergoplatform.NodeConnector
import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.persistance.*
import org.ergoplatform.utils.LogUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PersistanceTest {

    @Test
    fun main() {
        val database = setupDb()

        runBlocking {
            database.insertWalletConfig(
                WalletConfig(
                    0,
                    "Test",
                    "9xxx",
                    0,
                    null,
                    false,
                    null
                )
            )
        }

        val entities = database.getAllWalletConfigsSynchronous()
        println(entities.toString())

        runBlocking {
            try {
                database.withTransaction {
                    database.insertWalletConfig(WalletConfig(0, "Test2", "x9x", 0, null, false, null))

                    val entities = database.getAllWalletConfigsSynchronous()
                    println(entities.toString())

                    throw RuntimeException()
                }
            } catch (T: Throwable) {

            }
        }

        val entities2 = database.getAllWalletConfigsSynchronous()
        println(entities2.toString())

        assertEquals(entities.size, entities2.size)
    }

    @Test
    fun testObserving() {
        val database = setupDb()
        var changes = 0
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            database.getWalletsWithStatesFlow().collect {
                println(it)
                changes++
            }
        }

        val firstAddress = "firstaddress"
        runBlocking {
            database.insertWalletConfig(
                WalletConfig(
                    0,
                    "Observertest",
                    firstAddress,
                    0,
                    null,
                    false,
                    null
                )
            )
            delay(500)

            database.withTransaction {
                database.insertWalletConfig(
                    WalletConfig(
                        0,
                        "Observertest",
                        firstAddress + "2",
                        0,
                        null,
                        false,
                        null
                    )
                )
                database.insertWalletConfig(
                    WalletConfig(
                        0,
                        "Observertest",
                        firstAddress + "3",
                        0,
                        null,
                        false,
                        null
                    )
                )


            }
            delay(3000)

            // two changes: insertWalletConfig and transaction
            assertEquals(2, changes)
            val changeBeforeCancel = changes
            coroutineScope.cancel()
            database.insertWalletConfig(
                WalletConfig(
                    0,
                    "Observertest",
                    firstAddress + "4",
                    0,
                    null,
                    false,
                    null
                )
            )

            delay(2000)
            assertEquals(changeBeforeCancel, changes)
        }
    }

    fun testNodeConnector() {
        val db = setupDb()
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        isErgoMainNet = false
        coroutineScope.launch {
            db.getWalletsWithStatesFlow().collect {
                println(it)
            }
        }

        val prefs = mock<PreferencesProvider> {
        }
        whenever(prefs.prefDisplayCurrency).thenReturn("")
        whenever(prefs.prefExplorerApiUrl).thenReturn(getDefaultExplorerApiUrl())

        runBlocking {
            db.insertWalletConfig(WalletConfig(0, "Test2", "3Wwxnaem5ojTfp91qfLw3Y4Sr7ZWVcLPvYSzTsZ4LKGcoxujbxd3", 0, null, false, null))
            NodeConnector.getInstance().refreshByUser(prefs, db)
            delay(10000)
        }
    }

    private fun setupDb(): SqlDelightWalletProvider {
        LogUtils.logDebug = true
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        //val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:test.db")
        DbInitializer.initDbSchema(driver)
        DbInitializer.initDbSchema(driver)

        val database = SqlDelightWalletProvider(AppDatabase(driver))
        return database
    }
}