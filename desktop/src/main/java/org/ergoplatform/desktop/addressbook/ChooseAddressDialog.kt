package org.ergoplatform.desktop.addressbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import org.ergoplatform.Application
import org.ergoplatform.compose.addressbook.ChooseAddressLayout
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.persistance.IAddressWithLabel

@Composable
fun ChooseAddressDialog(
    onChooseEntry: (IAddressWithLabel) -> Unit,
    onEditEntry: (AddressBookEntry?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val addressesList =
        Application.database.addressBookDbProvider.getAllAddressEntries()
            .collectAsState(emptyList())

    val walletsWithStates =
        Application.database.walletDbProvider.getWalletsWithStatesFlow()
            .collectAsState(emptyList())

    AppDialog(onDismissRequest) {
        val scrollState = rememberScrollState()
        Box(Modifier.verticalScroll(scrollState)) {
            ChooseAddressLayout(
                walletsWithStates.value,
                addressesList.value,
                onChooseEntry,
                onEditEntry,
                Application.texts
            )
        }
    }
}