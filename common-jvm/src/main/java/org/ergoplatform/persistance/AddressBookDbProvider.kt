package org.ergoplatform.persistance

import kotlinx.coroutines.flow.Flow

interface AddressBookDbProvider {
    suspend fun loadAddressEntryById(id: Int): AddressBookEntry?
    suspend fun findAddressEntry(address: String): AddressBookEntry?
    suspend fun updateAddressEntry(addressBookEntry: AddressBookEntry)
    suspend fun deleteAddressEntry(id: Int)
    fun getAllAddressEntries(): Flow<List<AddressBookEntry>>
}