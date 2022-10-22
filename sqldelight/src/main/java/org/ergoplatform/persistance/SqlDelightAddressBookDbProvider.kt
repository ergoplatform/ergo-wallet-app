package org.ergoplatform.persistance

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqlDelightAddressBookDbProvider(
    private val sqlDelightAppDb: SqlDelightAppDb
) : AddressBookDbProvider {
    private val appDb = sqlDelightAppDb.appDatabase

    override suspend fun loadAddressEntryById(id: Int): AddressBookEntry? {
        return sqlDelightAppDb.useIoContext {
            appDb.addressBookQueries.loadAddressBookEntryById(id.toLong())
                .executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun updateAddressEntry(addressBookEntry: AddressBookEntry) {
        sqlDelightAppDb.useIoContext {
            appDb.addressBookQueries.insertOrReplace(
                if (addressBookEntry.id > 0) addressBookEntry.id.toLong() else null,
                addressBookEntry.label,
                addressBookEntry.address,
                addressBookEntry.signedData,
            )
        }
    }

    override suspend fun deleteAddressEntry(id: Int) {
        sqlDelightAppDb.useIoContext {
            appDb.addressBookQueries.deleteAddressById(id.toLong())
        }
    }

    override fun getAllAddressEntries(): Flow<List<AddressBookEntry>> {
        return appDb.addressBookQueries.selectAll().asFlow().mapToList()
            .map { flow -> flow.map { it.toModel() } }
    }

    override suspend fun findAddressEntry(address: String): AddressBookEntry? {
        return sqlDelightAppDb.useIoContext {
            appDb.addressBookQueries.findAddressBookEntry(address).executeAsOneOrNull()?.toModel()
        }
    }
}