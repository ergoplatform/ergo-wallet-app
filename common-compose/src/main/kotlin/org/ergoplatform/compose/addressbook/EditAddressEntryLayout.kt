package org.ergoplatform.compose.addressbook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.primaryButtonColors
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.addressbook.EditAddressEntryUiLogic

@Composable
fun EditAddressEntryLayout(
    addressBookEntry: AddressBookEntry,
    stringProvider: StringProvider,
    uiLogic: EditAddressEntryUiLogic,
    onSaved: () -> Unit,
) {
    val labelState = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(addressBookEntry.label))
    }
    val labelError = rememberSaveable { mutableStateOf(false) }
    val addressError = rememberSaveable { mutableStateOf(false) }
    val addressState = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(addressBookEntry.address))
    }
    val onDone = {
        val response = uiLogic.saveAddressEntry(labelState.value.text, addressState.value.text)
        labelError.value = response.labelError
        addressError.value = response.addressError
        if (response.hasSaved) onSaved()
    }

    Column(Modifier.padding(defaultPadding)) {
        Text(
            remember { stringProvider.getString(STRING_BUTTON_EDIT_ADDRESS_ENTRY) },
            Modifier.align(Alignment.CenterHorizontally),
            style = labelStyle(LabelStyle.BODY1BOLD)
        )

        OutlinedTextField(
            labelState.value,
            { textFieldValue -> labelState.value = textFieldValue },
            Modifier.padding(vertical = defaultPadding / 4).fillMaxWidth(),
            isError = labelError.value,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            label = { Text(stringProvider.getString(STRING_LABEL_DESCRIPTIVE_ADDRESS_NAME)) },
            colors = appTextFieldColors(),
        )

        // TODO read only for existing addresses
        OutlinedTextField(
            addressState.value,
            { textFieldValue -> addressState.value = textFieldValue },
            Modifier.padding(vertical = defaultPadding / 4).fillMaxWidth(),
            isError = addressError.value,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            singleLine = true,
            label = { Text(stringProvider.getString(STRING_LABEL_ERG_ADDRESS)) },
            colors = appTextFieldColors(),
        )

        // TODO add delete button

        AppButton(
            onClick = onDone,
            Modifier.align(Alignment.End).padding(top = defaultPadding),
            colors = primaryButtonColors(),
        ) {
            Text(remember { stringProvider.getString(STRING_BUTTON_SAVE) })
        }
    }
}