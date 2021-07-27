package org.ergoplatform.android

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.ergoplatform.android.wallet.WalletConfigDbEntity
import org.ergoplatform.android.wallet.WalletDbDao
import org.ergoplatform.android.wallet.WalletStateDbEntity

@Database(entities = arrayOf(WalletConfigDbEntity::class, WalletStateDbEntity::class), version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDbDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "ergowallet")
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `wallet_states`")
                // taken from AppDatabase_Impl :-)
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_states` (`public_address` TEXT NOT NULL, `transactions` INTEGER, `balance` INTEGER, `unconfirmed_balance` INTEGER, PRIMARY KEY(`public_address`))")
            }
        }

        // TODO drop wallet_states.transactions and create wallet_states.tokens on next migration
    }
}
