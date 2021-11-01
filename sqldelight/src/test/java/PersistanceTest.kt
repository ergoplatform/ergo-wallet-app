import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import org.ergoplatform.persistance.AppDatabase
import org.ergoplatform.persistance.SqlDelightWalletProvider
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.toDbEntity
import org.junit.Assert.*

import org.junit.Test
import java.lang.RuntimeException

class PersistanceTest {

    @Test
    fun main() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        //val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:test.db")
        AppDatabase.Schema.create(driver)

        val database = SqlDelightWalletProvider(AppDatabase(driver))
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
}