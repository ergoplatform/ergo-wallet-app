package org.ergoplatform.android

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ergoplatform.android.wallet.*
import org.ergoplatform.persistance.*

@Database(
    entities = arrayOf(
        WalletConfigDbEntity::class,
        WalletStateDbEntity::class,
        WalletAddressDbEntity::class,
        WalletTokenDbEntity::class
    ), version = 5
)
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
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .build()
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `wallet_states`")
                // taken from AppDatabase_Impl :-)
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_states` (`public_address` TEXT NOT NULL, `transactions` INTEGER, `balance` INTEGER, `unconfirmed_balance` INTEGER, PRIMARY KEY(`public_address`))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `wallet_states`")
                // taken from AppDatabase_Impl :-)
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_states` (`public_address` TEXT NOT NULL, `wallet_first_address` TEXT NOT NULL, `balance` INTEGER, `unconfirmed_balance` INTEGER, PRIMARY KEY(`public_address`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_addresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `wallet_first_address` TEXT NOT NULL, `index` INTEGER NOT NULL, `public_address` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_tokens` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `public_address` TEXT NOT NULL, `wallet_first_address` TEXT NOT NULL, `token_id` TEXT, `amount` INTEGER, `decimals` INTEGER, `name` TEXT)")
                database.execSQL("ALTER TABLE wallet_configs ADD COLUMN `unfold_tokens` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallet_addresses ADD COLUMN `label` TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallet_configs ADD COLUMN `xpubkey` TEXT")
            }
        }
    }
}

class RoomWalletDbProvider(val database: AppDatabase) : WalletDbProvider {
    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return database.withTransaction(block)
    }

    override suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfig? {
        return database.walletDao().loadWalletByFirstAddress(firstAddress)?.toModel()
    }

    override suspend fun loadWalletConfigById(id: Int): WalletConfig? {
        return database.walletDao().loadWalletConfigById(id)?.toModel()
    }

    override suspend fun updateWalletConfig(walletConfig: WalletConfig) {
        database.walletDao().update(walletConfig.toDbEntity())
    }

    override suspend fun insertWalletConfig(walletConfig: WalletConfig) {
        database.walletDao().insertAll(walletConfig.toDbEntity())
    }

    override suspend fun deleteWalletConfigAndStates(firstAddress: String, walletId: Int?) {
        database.walletDao().deleteWalletStates(firstAddress)
        database.walletDao().deleteTokensByWallet(firstAddress)
        database.walletDao().deleteWalletAddresses(firstAddress)
        (walletId ?: database.walletDao().loadWalletByFirstAddress(firstAddress)?.id)?.let { id ->
            database.walletDao().deleteWalletConfig(id)
        }
    }

    override fun getAllWalletConfigsSynchronous(): List<WalletConfig> {
        return database.walletDao().getAllWalletConfigsSyncronous().map { it.toModel() }
    }

    override suspend fun loadWalletWithStateById(id: Int): Wallet? {
        return database.walletDao().loadWalletWithStateById(id)?.toModel()
    }

    override suspend fun walletWithStateByIdAsFlow(id: Int): Flow<Wallet?> {
        return database.walletDao().walletWithStateByIdAsFlow(id).map { it?.toModel() }
    }

    override suspend fun insertWalletStates(walletStates: List<WalletState>) {
        database.walletDao()
            .insertWalletStates(*(walletStates.map { it.toDbEntity() }.toTypedArray()))
    }

    override suspend fun deleteAddressState(publicAddress: String) {
        database.walletDao().deleteAddressState(publicAddress)
    }

    override suspend fun loadWalletAddresses(firstAddress: String): List<WalletAddress> {
        return database.walletDao().loadWalletAddresses(firstAddress).map { it.toModel() }
    }

    override suspend fun loadWalletAddress(id: Long): WalletAddress? {
        return database.walletDao().loadWalletAddress(id.toInt())?.toModel()
    }

    override suspend fun loadWalletAddress(publicAddress: String): WalletAddress? {
        return database.walletDao().loadWalletAddress(publicAddress)?.toModel()
    }

    override suspend fun insertWalletAddress(walletAddress: WalletAddress) {
        database.walletDao().insertWalletAddress(walletAddress.toDbEntity())
    }

    override suspend fun updateWalletAddressLabel(addrId: Long, newLabel: String?) {
        database.walletDao().updateWalletAddressLabel(addrId.toInt(), newLabel)
    }

    override suspend fun deleteWalletAddress(addrId: Long) {
        database.walletDao().deleteWalletAddress(addrId.toInt())
    }

    override suspend fun deleteTokensByAddress(publicAddress: String) {
        database.walletDao().deleteTokensByAddress(publicAddress)
    }

    override suspend fun insertWalletTokens(walletTokens: List<WalletToken>) {
        database.walletDao()
            .insertWalletTokens(*(walletTokens.map { it.toDbEntity() }.toTypedArray()))
    }
}