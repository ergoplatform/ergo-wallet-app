package org.ergoplatform.desktop.addressbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.persistance.IAddressWithLabel

class AddressBookDialogStateHandler {
    private val chooseAddressDialog = mutableStateOf(false)
    private val editAddressDialog = mutableStateOf<DesktopEditEntryUiLogic?>(null)

    fun showChooseAddressDialog() {
        chooseAddressDialog.value = true
    }

    @Composable
    fun AddressBookDialogs(
        onChooseEntry: (IAddressWithLabel) -> Unit,
        componentScope: CoroutineScope,
    ) {
        if (editAddressDialog.value != null) {
            EditAddressDialog(
                editAddressDialog.value!!,
                onDismissRequest = { editAddressDialog.value = null }
            )
        } else if (chooseAddressDialog.value) {
            ChooseAddressDialog(
                onChooseEntry = { addressWithLabel ->
                    chooseAddressDialog.value = false
                    onChooseEntry(addressWithLabel)
                },
                onEditEntry = {
                    editAddressDialog.value =
                        DesktopEditEntryUiLogic(it) { componentScope }
                },
                onDismissRequest = { chooseAddressDialog.value = false }
            )
        }
    }
}