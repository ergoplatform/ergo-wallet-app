package org.ergoplatform.desktop.addressbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.compose.addressbook.EditAddressEntryLayout
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.uilogic.addressbook.EditAddressEntryUiLogic

@Composable
fun EditAddressDialog(
    uiLogic: DesktopEditEntryUiLogic,
    onDismissRequest: () -> Unit,
) {
    AppDialog(onDismissRequest) {
        val scrollState = rememberScrollState()
        Box(Modifier.verticalScroll(scrollState)) {
            EditAddressEntryLayout(
                uiLogic.adressEntryState.value,
                Application.texts,
                uiLogic,
                onDismissRequest,
                getAppDB = { Application.database.addressBookDbProvider }
            )
        }
    }
}

class DesktopEditEntryUiLogic(
    addressBookEntry: AddressBookEntry?,
    private val componentScope: () -> CoroutineScope
) : EditAddressEntryUiLogic() {
    val adressEntryState = mutableStateOf(addressEntry)

    override fun coroutineScope(): CoroutineScope = componentScope()

    override fun notifyNewValue(value: AddressBookEntry) {
        adressEntryState.value = value
    }

    init {
        init(addressBookEntry?.id ?: 0, Application.database.addressBookDbProvider)
    }
}