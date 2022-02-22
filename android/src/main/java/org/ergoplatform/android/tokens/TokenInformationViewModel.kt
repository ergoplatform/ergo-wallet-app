package org.ergoplatform.android.tokens

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.TokenInfoManager
import org.ergoplatform.android.Preferences

class TokenInformationViewModel : ViewModel() {
    private var tokenId: String? = null
    private val _tokenInfo = MutableLiveData<TokenInfoManager.TokenInformationAndEip4Token?>()
    val tokenInfo: LiveData<TokenInfoManager.TokenInformationAndEip4Token?> = _tokenInfo

    fun init(tokenId: String, ctx: Context) {
        if (this.tokenId != null)
            return

        this.tokenId = tokenId

        viewModelScope.launch {
            val info = TokenInfoManager.getTokenInformation(
                tokenId,
                ErgoApiService.getOrInit(Preferences(ctx))
            )
            _tokenInfo.postValue(info)
        }
    }
}