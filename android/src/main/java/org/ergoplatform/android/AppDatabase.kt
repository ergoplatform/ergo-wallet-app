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
import org.ergoplatform.android.tokens.TokenDbDao
import org.ergoplatform.android.tokens.TokenInformationDbEntity
import org.ergoplatform.android.tokens.TokenPriceDbEntity
import org.ergoplatform.android.tokens.toDbEntity
import org.ergoplatform.android.transactions.AddressTransactionDbEntity
import org.ergoplatform.android.transactions.AddressTransactionTokenDbEntity
import org.ergoplatform.android.transactions.TransactionDbDao
import org.ergoplatform.android.transactions.toDbEntity
import org.ergoplatform.android.wallet.*
import org.ergoplatform.persistance.*

@Database(
    entities = [
        WalletConfigDbEntity::class,
        WalletStateDbEntity::class,
        WalletAddressDbEntity::class,
        WalletTokenDbEntity::class,
        AddressTransactionDbEntity::class,
        AddressTransactionTokenDbEntity::class,
        TokenPriceDbEntity::class,
        TokenInformationDbEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(), IAppDatabase {
    abstract fun walletDao(): WalletDbDao
    abstract fun tokenDao(): TokenDbDao
    abstract fun transactionDao(): TransactionDbDao

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
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .build()
        }

        /**
         * MIGRATIONS
         *
         * CREATE TABLE statements are taken right away from build/generated/**/AppDatabase_Impl.java
         */

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `wallet_states`")
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_states` (`public_address` TEXT NOT NULL, `transactions` INTEGER, `balance` INTEGER, `unconfirmed_balance` INTEGER, PRIMARY KEY(`public_address`))")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `wallet_states`")
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_states` (`public_address` TEXT NOT NULL, `wallet_first_address` TEXT NOT NULL, `balance` INTEGER, `unconfirmed_balance` INTEGER, PRIMARY KEY(`public_address`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_addresses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `wallet_first_address` TEXT NOT NULL, `index` INTEGER NOT NULL, `public_address` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `wallet_tokens` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `public_address` TEXT NOT NULL, `wallet_first_address` TEXT NOT NULL, `token_id` TEXT, `amount` INTEGER, `decimals` INTEGER, `name` TEXT)")
                database.execSQL("ALTER TABLE wallet_configs ADD COLUMN `unfold_tokens` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallet_addresses ADD COLUMN `label` TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE wallet_configs ADD COLUMN `xpubkey` TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `token_price` (`tokenId` TEXT NOT NULL, `display_name` TEXT, `source` TEXT NOT NULL, `erg_value` TEXT NOT NULL, PRIMARY KEY(`tokenId`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `token_info` (`tokenId` TEXT NOT NULL, `issuing_box` TEXT NOT NULL, `minting_tx` TEXT NOT NULL, `display_name` TEXT NOT NULL, `description` TEXT NOT NULL, `decimals` INTEGER NOT NULL, `full_supply` INTEGER NOT NULL, `reg7` TEXT, `reg8` TEXT, `reg9` TEXT, `genuine_flag` INTEGER NOT NULL, `issuer_link` TEXT, `thumbnail_bytes` BLOB, `thunbnail_type` INTEGER NOT NULL, `updated_ms` INTEGER NOT NULL, PRIMARY KEY(`tokenId`))")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_states_wallet_first_address` ON `wallet_states` (`wallet_first_address`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_addresses_wallet_first_address` ON `wallet_addresses` (`wallet_first_address`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_tokens_wallet_first_address` ON `wallet_tokens` (`wallet_first_address`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_tokens_public_address` ON `wallet_tokens` (`public_address`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `address_transaction` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `address` TEXT NOT NULL, `tx_id` TEXT NOT NULL, `inclusion_height` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `nanoerg` INTEGER NOT NULL, `message` TEXT, `state` INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_address_transaction_address_inclusion_height` ON `address_transaction` (`address` ASC, `inclusion_height` DESC)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `address_transaction_token` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `address` TEXT NOT NULL, `tx_id` TEXT NOT NULL, `token_id` TEXT NOT NULL, `name` TEXT NOT NULL, `amount` INTEGER NOT NULL, `decimals` INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_address_transaction_token_address_tx_id` ON `address_transaction_token` (`address`, `tx_id`)")
            }
        }
    }

    override val tokenDbProvider get() = RoomTokenDbProvider(this)
    override val walletDbProvider get() = RoomWalletDbProvider(this)
    override val transactionDbProvider: TransactionDbProvider
        get() = RoomTransactionDbProvider(this)
}

class RoomWalletDbProvider(private val database: AppDatabase) : WalletDbProvider {
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
        // TODO transactionlist delete from db
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

class RoomTokenDbProvider(private val database: AppDatabase) : TokenDbProvider {
    override suspend fun loadTokenPrices(): List<TokenPrice> {
        return database.tokenDao().getAllTokenPrices().map { it.toModel() }
    }

    override suspend fun updateTokenPrices(priceList: List<TokenPrice>) {
        database.withTransaction {
            database.tokenDao().deleteAllTokenPrices()
            database.tokenDao()
                .insertTokenPrices(*(priceList.map { it.toDbEntity() }.toTypedArray()))
        }
    }

    override suspend fun loadTokenInformation(tokenId: String): TokenInformation? {
        return database.tokenDao().getTokenInformation(tokenId)?.toModel()
    }

    override suspend fun insertOrReplaceTokenInformation(tokenInfo: TokenInformation) {
        database.tokenDao().insertOrUpdateTokenInformation(tokenInfo.toDbEntity())
    }

    override suspend fun pruneUnusedTokenInformation() {
        database.tokenDao()
            .deleteOutdatedTokenInformation(System.currentTimeMillis() - TOKEN_INFO_MS_OUTDATED)
    }

}

class RoomTransactionDbProvider(private val database: AppDatabase) : TransactionDbProvider {
    override suspend fun insertOrUpdateAddressTransaction(addressTransaction: AddressTransaction) {
        database.transactionDao().insertOrUpdateAddressTransaction(addressTransaction.toDbEntity())
    }

    override suspend fun loadAddressTransactions(
        address: String,
        limit: Int,
        page: Int
    ): List<AddressTransaction> {
        return database.transactionDao().loadAddressTransactions(address, limit, page)
            .map { it.toModel() }
    }

    override suspend fun deleteAddressTransactions(address: String) {
        database.transactionDao().deleteAddressTransactions(address)
        database.transactionDao().deleteAddressTransactionTokens(address)
    }

    override suspend fun deleteTransaction(id: Int) {
        database.transactionDao().apply {
            loadAddressTransaction(id)?.let { addressTransaction ->
                deleteAddressTransactionTokens(addressTransaction.address, addressTransaction.txId)
                deleteAddressTransaction(addressTransaction.id)
            }
        }
    }

    override suspend fun insertOrUpdateAddressTransactionToken(addressTxToken: AddressTransactionToken) {
        database.transactionDao().insertOrUpdateAddressTransactionToken(addressTxToken.toDbEntity())
    }

    override suspend fun loadAddressTransactionTokens(
        address: String,
        txId: String
    ): List<AddressTransactionToken> {
        return database.transactionDao().loadAddressTransactionTokens(address, txId)
            .map { it.toModel() }
    }

}