package org.ergoplatform.persistance

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

    override suspend fun getAllAddressEntries(): List<AddressBookEntry> {
        return sqlDelightAppDb.useIoContext {
            appDb.addressBookQueries.selectAll().executeAsList().map { it.toModel() }
        }
    }

    override suspend fun findAddressEntry(address: String): AddressBookEntry? {
        return sqlDelightAppDb.useIoContext {
            appDb.addressBookQueries.findAddressBookEntry(address).executeAsOneOrNull()?.toModel()
        }
    }
}