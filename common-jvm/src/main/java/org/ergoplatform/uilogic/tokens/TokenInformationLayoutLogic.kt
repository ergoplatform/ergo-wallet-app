package org.ergoplatform.uilogic.tokens

import org.ergoplatform.TokenAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NONE
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.uilogic.STRING_LABEL_NONE
import org.ergoplatform.uilogic.STRING_LABEL_NO_DESCRIPTION
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.formatTokenPriceToString
import org.ergoplatform.utils.toHex

abstract class TokenInformationLayoutLogic {
    fun updateLayout(
        uiLogic: TokenInformationModelLogic,
        texts: StringProvider,
        rawBalanceAmount: Long
    ) {
        uiLogic.tokenInformation?.apply {
            setTokenTextFields(
                if (displayName.isBlank()) texts.getString(STRING_LABEL_UNNAMED_TOKEN) else displayName,
                tokenId,
                if (description.isNotBlank()) description else texts.getString(
                    STRING_LABEL_NO_DESCRIPTION
                )
            )
            setTokenGenuine(genuineFlag)

            val balanceAmount = TokenAmount(rawBalanceAmount, decimals)

            val showBalance = balanceAmount.rawValue > 0 && !isSingularToken()
            val walletSyncManager = WalletStateSyncManager.getInstance()
            val tokenPrice = walletSyncManager.tokenPrices[tokenId]

            setLabelSupplyAmountText(
                if (!isSingularToken()) TokenAmount(fullSupply, decimals).toStringUsFormatted(false)
                else null
            )

            val balanceValue = if (showBalance) tokenPrice?.let {
                formatTokenPriceToString(
                    balanceAmount,
                    it.ergValue,
                    walletSyncManager,
                    texts
                ) + " [${it.priceSource}]"
            } else null

            setBalanceAmountAndValue(
                if (showBalance) balanceAmount.toStringUsFormatted(false) else null,
                balanceValue
            )

            updateNftLayout(uiLogic, texts)
        }
    }

    private fun updateNftLayout(
        uiLogic: TokenInformationModelLogic,
        texts: StringProvider
    ) {
        // NFT information
        val isNft = uiLogic.eip4Token?.isNftAssetType ?: false
        setNftLayoutVisibility(isNft)

        if (isNft) {
            val eip4Token = uiLogic.eip4Token!!
            setContentLinkText(eip4Token.nftContentLink ?: texts.getString(STRING_LABEL_NONE))

            setContentHashText(eip4Token.nftContentHash?.let { it.toHex() + " [SHA256]" }
                ?: texts.getString(STRING_LABEL_NONE))

            setThumbnail(
                if (uiLogic.downloadState != TokenInformationModelLogic.StateDownload.NOT_AVAILABLE) THUMBNAIL_TYPE_NONE
                else uiLogic.tokenInformation?.thumbnailType ?: 0
            )

            // show preview layout when download is running or when picture is ready
            updateNftPreview(uiLogic)
        }
    }

    fun updateNftPreview(uiLogic: TokenInformationModelLogic) {
        showNftPreview(uiLogic.downloadState, uiLogic.downloadPercent, uiLogic.downloadedData)
        showNftHashValidation(uiLogic.sha256Check)
    }

    abstract fun setTokenTextFields(displayName: String, tokenId: String, description: String)

    abstract fun setLabelSupplyAmountText(supplyAmount: String?)

    abstract fun setTokenGenuine(genuineFlag: Int)

    abstract fun setBalanceAmountAndValue(amount: String?, balanceValue: String?)

    abstract fun setContentLinkText(linkText: String)

    abstract fun setNftLayoutVisibility(visible: Boolean)

    abstract fun setContentHashText(hashText: String)

    abstract fun setThumbnail(thumbnailType: Int)

    abstract fun showNftPreview(
        downloadState: TokenInformationModelLogic.StateDownload,
        downloadPercent: Float,
        content: ByteArray?
    )

    /**
     * @param hashValid true if saved hash is the same as content hash, false if not. `null` if
     *                  no comparison was made so far (due to saved hash not available or
     *                  content hash not yet available)
     */
    abstract fun showNftHashValidation(hashValid: Boolean?)
}
