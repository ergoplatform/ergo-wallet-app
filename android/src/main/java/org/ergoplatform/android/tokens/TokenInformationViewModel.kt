package org.ergoplatform.android.tokens

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.persistance.TokenInformation

class TokenInformationViewModel : ViewModel() {
    private var tokenId: String? = null
    private val _tokenInfo = MutableLiveData<TokenInformation?>()
    val tokenInfo: LiveData<TokenInformation?> = _tokenInfo

    fun init(tokenId: String, ctx: Context) {
        if (this.tokenId != null)
            return

        this.tokenId = tokenId

        viewModelScope.launch {
            val info = TokenInfoManager.getInstance().getTokenInformation(
                tokenId,
                AppDatabase.getInstance(ctx).tokenDbProvider,
                ErgoApiService.getOrInit(Preferences(ctx))
            )
            _tokenInfo.postValue(info)
        }
    }
}