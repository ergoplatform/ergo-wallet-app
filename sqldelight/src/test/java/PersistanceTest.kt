import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.ergoplatform.persistance.*
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistanceTest {

    @Test
    fun main() {
        val database = setupDb()

        runBlocking {
            database.insertWalletConfig(
                WalletConfig(
                    1,
                    "Test",
                    "9xxx",
                    0,
                    null,
                    false
                )
            )
        }

        val entities = database.getAllWalletConfigsSynchronous()
        println(entities.toString())

        runBlocking {
            try {
                database.withTransaction {
                    database.insertWalletConfig(WalletConfig(0, "Test2", "x9x", 0, null, false))

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
            database.getWalletsWithStates().collect {
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
                    false
                )
            )
            delay(200)

            database.withTransaction {
                database.deleteTokensByAddress(firstAddress)
                delay(200)
                database.insertWalletStates(listOf(WalletState(firstAddress, firstAddress, 1L, 0L)))
                delay(200)
                database.insertWalletTokens(
                    listOf(
                        WalletToken(
                            0,
                            firstAddress,
                            firstAddress,
                            "tokenid",
                            1L,
                            0,
                            "tokenname"
                        )
                    )
                )
            }
            delay(200)

            // two changes: insertWalletConfig and transaction
            assertEquals(2, changes)
            val changeBeforeCancel = changes
            coroutineScope.cancel()
            database.deleteTokensByAddress(firstAddress)
            delay(1000)
            assertEquals(changeBeforeCancel, changes)
        }
    }

    private fun setupDb(): SqlDelightWalletProvider {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        //val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:test.db")
        AppDatabase.Schema.create(driver)

        val database = SqlDelightWalletProvider(AppDatabase(driver))
        return database
    }
}