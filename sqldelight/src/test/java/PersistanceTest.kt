import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.ergoplatform.persistance.AppDatabase
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.toDbEntity
import org.junit.Assert.*

import org.junit.Test

class PersistanceTest {

    @Test
    fun main() {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        //val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:test.db")
        AppDatabase.Schema.create(driver)

        val database = AppDatabase(driver)
        database.walletConfigQueries.insertOrReplace(
            WalletConfig(
                1,
                "Test",
                "9xxx",
                0,
                null,
                false
            ).toDbEntity()
        )

        val entities = database.walletConfigQueries.selectAll().executeAsList()
        println(entities.toString())
    }
}