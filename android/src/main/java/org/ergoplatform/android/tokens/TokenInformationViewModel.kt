package org.ergoplatform.android.tokens

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.uilogic.tokens.TokenInformationUiLogic

class TokenInformationViewModel : ViewModel() {
    val uiLogic = AndroidUiLogic()

    private var tokenId: String? = null
    private val _tokenInfo = MutableLiveData<TokenInformation?>()
    val tokenInfo: LiveData<TokenInformation?> = _tokenInfo
    private val _downloadState = MutableLiveData<TokenInformationUiLogic.StateDownload>()
    val downloadState: LiveData<TokenInformationUiLogic.StateDownload> get() = _downloadState

    fun init(tokenId: String, ctx: Context) {
        if (this.tokenId != null)
            return

        this.tokenId = tokenId

        uiLogic.init(tokenId, AppDatabase.getInstance(ctx), Preferences(ctx))
    }

    inner class AndroidUiLogic : TokenInformationUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun onTokenInfoUpdated(tokenInformation: TokenInformation?) {
            _tokenInfo.postValue(tokenInformation)
        }

        override fun onDownloadStateUpdated() {
            _downloadState.postValue(downloadState)
        }

    }
}