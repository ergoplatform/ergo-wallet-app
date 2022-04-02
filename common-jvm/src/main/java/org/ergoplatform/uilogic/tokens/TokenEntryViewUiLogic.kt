package org.ergoplatform.uilogic.tokens

import org.ergoplatform.TokenAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.persistance.*
import org.ergoplatform.toTokenAmount
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.formatTokenPriceToString

abstract class TokenEntryViewUiLogic(val walletToken: WalletToken) {
    abstract val texts: StringProvider

    private var tokenInformation: TokenInformation? = null
    val tokenId get() = walletToken.tokenId

    abstract fun setDisplayedTokenName(tokenName: String)
    abstract fun setDisplayedTokenId(tokenId: String?)
    abstract fun setDisplayedBalance(value: String?)
    abstract fun setDisplayedPrice(price: String?)
    abstract fun setGenuineFlag(genuineFlag: Int)
    abstract fun setThumbnail(thumbnailType: Int)

    fun bind(tokenInformation: TokenInformation? = null) {
        this.tokenInformation = tokenInformation

        val textProvider = this.texts
        val balanceAmount =
            tokenInformation?.let { TokenAmount(walletToken.amount ?: 0, it.decimals) }
                ?: walletToken.toTokenAmount()
        val singularToken = (tokenInformation?.isSingularToken() ?: walletToken.isSingularToken())
        val genuineFlag = tokenInformation?.genuineFlag ?: GENUINE_UNKNOWN

        setDisplayedTokenName(
            tokenInformation?.displayName ?: walletToken.name ?: textProvider.getString(
                STRING_LABEL_UNNAMED_TOKEN
            )
        )
        setGenuineFlag(genuineFlag)
        setDisplayedTokenId(if (genuineFlag == GENUINE_VERIFIED) null else walletToken.tokenId)
        setDisplayedBalance(if (singularToken) null else balanceAmount.toStringPrettified())

        setDisplayedPrice(if (!singularToken) {
            val walletSyncManager = WalletStateSyncManager.getInstance()
            val tokenPrice = walletSyncManager.tokenPrices[tokenId]
            tokenPrice?.let {
                formatTokenPriceToString(
                    balanceAmount,
                    it.ergValue,
                    walletSyncManager,
                    textProvider
                ) + " [${it.priceSource}]"
            }
        } else null)

        setThumbnail(tokenInformation?.thumbnailType ?: THUMBNAIL_TYPE_NONE)
    }

    fun addTokenInfo(tokenInformation: TokenInformation) {
        if (this.tokenInformation == null || this.tokenInformation!!.updatedMs != tokenInformation.updatedMs) {
            bind(tokenInformation)
        }
    }
}