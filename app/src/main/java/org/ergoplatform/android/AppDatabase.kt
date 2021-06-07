package org.ergoplatform.android

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.ergoplatform.android.wallet.WalletDbDao
import org.ergoplatform.android.wallet.WalletConfigDbEntity
import org.ergoplatform.android.wallet.WalletStateDbEntity

@Database(entities = arrayOf(WalletConfigDbEntity::class, WalletStateDbEntity::class), version = 1)
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
                .build()
        }
    }
}
