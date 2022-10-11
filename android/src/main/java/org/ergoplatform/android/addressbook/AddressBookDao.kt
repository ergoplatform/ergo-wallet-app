package org.ergoplatform.android.addressbook

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AddressBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg addressEntries: AddressBookEntryEntity)

    @Query("DELETE FROM address_book WHERE id = :addressId")
    suspend fun deleteAddressEntry(addressId: Int)

    @Query("SELECT * FROM address_book WHERE id = :id")
    suspend fun loadAddressEntryById(id: Int): AddressBookEntryEntity?

    @Query("SELECT * FROM address_book")
    suspend fun getAllAddressEntries(): List<AddressBookEntryEntity>
}