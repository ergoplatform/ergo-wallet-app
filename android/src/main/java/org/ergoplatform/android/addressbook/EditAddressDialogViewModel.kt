package org.ergoplatform.android.addressbook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.uilogic.addressbook.EditAddressEntryUiLogic

class EditAddressDialogViewModel : ViewModel() {
    val uiLogic = object : EditAddressEntryUiLogic() {
        override fun coroutineScope(): CoroutineScope = viewModelScope

        override fun notifyNewValue(value: AddressBookEntry) {
            _addressLiveData.postValue(value)
        }
    }

    private val _addressLiveData: MutableLiveData<AddressBookEntry> =
        MutableLiveData(uiLogic.addressEntry)
    val addressLiveData: LiveData<AddressBookEntry> get() = _addressLiveData
}