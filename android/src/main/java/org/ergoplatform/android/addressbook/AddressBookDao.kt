package org.ergoplatform.android.addressbook

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg addressEntries: AddressBookEntryEntity)

    @Query("DELETE FROM address_book WHERE id = :addressId")
    suspend fun deleteAddressEntry(addressId: Int)

    @Query("SELECT * FROM address_book WHERE id = :id")
    suspend fun loadAddressEntryById(id: Int): AddressBookEntryEntity?

    @Query("SELECT * FROM address_book WHERE address = :address")
    suspend fun findByAddress(address: String): AddressBookEntryEntity?

    @Query("SELECT * FROM address_book")
    fun getAllAddressEntries(): Flow<List<AddressBookEntryEntity>>
}