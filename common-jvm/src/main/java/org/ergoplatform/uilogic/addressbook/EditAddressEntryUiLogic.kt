package org.ergoplatform.uilogic.addressbook

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.persistance.AddressBookDbProvider
import org.ergoplatform.persistance.AddressBookEntry

abstract class EditAddressEntryUiLogic {
    private var initialized = false
    var addressEntry = AddressBookEntry(0, "", "", null)
        private set(value) {
            field = value
            notifyNewValue(value)
        }

    val isAddressReadOnly: Boolean get() = addressEntry.id > 0

    val canDeleteAddress: Boolean get() = addressEntry.id > 0

    fun init(addressId: Int, dbProvider: AddressBookDbProvider) {
        if (!initialized) {
            initialized = true
            if (addressId > 0)
                coroutineScope().launch {
                    dbProvider.loadAddressEntryById(addressId)?.let { addressEntry = it }
                }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun saveAddressEntry(
        newLabel: String,
        newAddress: String,
        dbProvider: AddressBookDbProvider
    ): CheckCanSaveResponse {
        val addressOk = isValidErgoAddress(newAddress)
        val labelOk = newLabel.isNotBlank()
        val canSave = addressOk && labelOk

        if (canSave) {
            GlobalScope.launch {
                val existingEntry =
                    if (!isAddressReadOnly) dbProvider.findAddressEntry(newAddress) else null
                val addressBookEntry = existingEntry ?: addressEntry

                val toSave = AddressBookEntry(
                    addressBookEntry.id,
                    newLabel,
                    newAddress,
                    null
                )

                dbProvider.updateAddressEntry(toSave)
            }
        }

        return CheckCanSaveResponse(canSave, !addressOk, !labelOk)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun deleteAddress(dbProvider: AddressBookDbProvider) {
        GlobalScope.launch {
            dbProvider.deleteAddressEntry(addressEntry.id)
        }
    }

    abstract fun coroutineScope(): CoroutineScope

    abstract fun notifyNewValue(value: AddressBookEntry)

    data class CheckCanSaveResponse(
        val hasSaved: Boolean,
        val addressError: Boolean,
        val labelError: Boolean,
    )
}