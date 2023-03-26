package org.ergoplatform.android.multisig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.uilogic.multisig.MultisigTxDetailsUiLogic

class MultisigTxDetailViewModel : ViewModel() {
    val uiLogic = AndroidMultisigTxDetailsUiLogic()

    inner class AndroidMultisigTxDetailsUiLogic : MultisigTxDetailsUiLogic() {
        override val coroutineScope: CoroutineScope = viewModelScope
    }
}