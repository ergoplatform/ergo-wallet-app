package org.ergoplatform.android.tokens

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApiService
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.persistance.TokenInformation

class TokenInformationViewModel : ViewModel() {
    private var tokenId: String? = null
    private val _tokenInfo = MutableLiveData<TokenInformation?>()
    val tokenInfo: LiveData<TokenInformation?> = _tokenInfo
    var eip4Token: Eip4Token? = null
        private set

    fun init(tokenId: String, ctx: Context) {
        if (this.tokenId != null)
            return

        this.tokenId = tokenId

        viewModelScope.launch {
            TokenInfoManager.getInstance().getTokenInformationFlow(
                tokenId,
                AppDatabase.getInstance(ctx).tokenDbProvider,
                ErgoApiService.getOrInit(Preferences(ctx))
            ).collect {
                eip4Token = try {
                    it?.toEip4Token()
                } catch (t: Throwable) {
                    null
                }

                _tokenInfo.postValue(it)
            }
        }
    }
}