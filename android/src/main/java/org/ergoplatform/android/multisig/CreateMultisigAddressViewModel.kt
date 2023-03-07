package org.ergoplatform.android.multisig

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import org.ergoplatform.uilogic.multisig.CreateMultisigAddressUiLogic

class CreateMultisigAddressViewModel : ViewModel() {
    val uiLogic = CreateMultisigAddressUiLogic()

    val participantAddress = mutableStateOf(TextFieldValue())
}