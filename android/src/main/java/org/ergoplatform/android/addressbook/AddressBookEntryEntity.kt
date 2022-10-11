package org.ergoplatform.android.addressbook

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.ergoplatform.persistance.AddressBookEntry

@Entity(tableName = "address_book")
data class AddressBookEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "signed_data") val signedData: ByteArray?,
) {
    fun toModel(): AddressBookEntry =
        AddressBookEntry(id, label, address, signedData)
}

fun AddressBookEntry.toDbEntitiy() = AddressBookEntryEntity(
    id, label, address, signedData
)