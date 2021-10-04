package org.ergoplatform.android.wallet

import androidx.lifecycle.ViewModel
import org.ergoplatform.appkit.Mnemonic
import org.ergoplatform.appkit.SecretString

class CreateWalletViewModel : ViewModel() {
    private var _mnemonic: SecretString? = null

    val mnemonic get() = run {
        if (_mnemonic == null) {
            _mnemonic = SecretString.create(Mnemonic.generateEnglishMnemonic())
        }

        _mnemonic!!.toStringUnsecure()
    }

    override fun onCleared() {
        _mnemonic?.erase()
        super.onCleared()
    }
}