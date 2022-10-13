package org.ergoplatform.uilogic.addressbook

import kotlinx.coroutines.CoroutineScope
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

    fun init(addressId: Int, dbProvider: AddressBookDbProvider) {
        if (!initialized) {
            initialized = true
            if (addressId > 0)
                coroutineScope().launch {
                    dbProvider.loadAddressEntryById(addressId)?.let { addressEntry = it }
                }
        }
    }

    fun saveAddressEntry(newLabel: String, newAddress: String): CheckCanSaveResponse {
        val addressOk = isValidErgoAddress(newAddress)
        val labelOk = newLabel.isNotBlank()
        val canSave = addressOk && labelOk

        if (canSave) {
            // TODO save in db
            // TODO check if already in AB, overwrite existing entry then
        }

        return CheckCanSaveResponse(canSave, !addressOk, !labelOk)
    }

    abstract fun coroutineScope(): CoroutineScope

    abstract fun notifyNewValue(value: AddressBookEntry)

    data class CheckCanSaveResponse(
        val hasSaved: Boolean,
        val addressError: Boolean,
        val labelError: Boolean,
    )
}