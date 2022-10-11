package org.ergoplatform.compose.addressbook

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.uilogic.STRING_BUTTON_ADD_ADDRESS_ENTRY
import org.ergoplatform.uilogic.STRING_LABEL_NO_ENTRIES
import org.ergoplatform.uilogic.StringProvider

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChooseAddressLayout(
    addressesList: List<AddressBookEntry>,
    onChooseEntry: (AddressBookEntry) -> Unit,
    onEditEntry: (AddressBookEntry?) -> Unit,
    onDismissRequest: () -> Unit,
    stringProvider: StringProvider,
) {
    Column(
        Modifier.fillMaxWidth().padding(defaultPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            remember { stringProvider.getString(STRING_BUTTON_ADD_ADDRESS_ENTRY) },
            Modifier.clickable { onEditEntry(null) }.padding(defaultPadding / 2),
            style = labelStyle(LabelStyle.BODY1BOLD),
        )
        Divider(Modifier.padding(vertical = defaultPadding / 2))
        addressesList.forEach { addressEntry ->
            key(addressEntry.id) {
                Column(
                    Modifier.combinedClickable(
                        onClick = { onChooseEntry(addressEntry) },
                        onLongClick = { onEditEntry(addressEntry) },
                    ).padding(defaultPadding), horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        addressEntry.label,
                        style = labelStyle(LabelStyle.BODY1BOLD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    MiddleEllipsisText(
                        addressEntry.address,
                        style = labelStyle(LabelStyle.BODY2),
                    )

                }
            }
        }
        if (addressesList.isEmpty()) {
            Text(
                remember { stringProvider.getString(STRING_LABEL_NO_ENTRIES) },
                Modifier.padding(defaultPadding),
                style = labelStyle(LabelStyle.BODY1)
            )
        }
    }
}