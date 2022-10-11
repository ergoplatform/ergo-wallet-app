package org.ergoplatform.persistance

interface AddressBookDbProvider {
    suspend fun loadAddressEntryById(id: Int): AddressBookEntry?
    suspend fun findAddressEntry(address: String): AddressBookEntry?
    suspend fun updateAddressEntry(addressBookEntry: AddressBookEntry)
    suspend fun deleteAddressEntry(id: Int)
    suspend fun getAllAddressEntries(): List<AddressBookEntry>
}