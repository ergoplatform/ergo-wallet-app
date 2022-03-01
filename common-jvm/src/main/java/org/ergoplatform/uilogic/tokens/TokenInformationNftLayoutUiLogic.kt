package org.ergoplatform.uilogic.tokens

import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NONE
import org.ergoplatform.tokens.getHttpContentLink
import org.ergoplatform.uilogic.STRING_LABEL_NONE
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.toHex

abstract class TokenInformationNftLayoutUiLogic {
    fun update(
        uiLogic: TokenInformationUiLogic,
        texts: StringProvider,
        preferencesProvider: PreferencesProvider,
        openUrlWithBrowser: (String) -> Boolean
    ) {
        // NFT information
        val isNft = uiLogic.eip4Token?.isNftAssetType ?: false
        setNftLayoutVisibility(isNft)

        if (isNft) {
            val eip4Token = uiLogic.eip4Token!!
            setContentLinkText(eip4Token.nftContentLink ?: texts.getString(STRING_LABEL_NONE))
            setContentLinkClickListener(eip4Token.nftContentLink?.let {
                Runnable {
                    val success = openUrlWithBrowser(eip4Token.nftContentLink!!)

                    if (!success) {
                        eip4Token.getHttpContentLink(preferencesProvider)?.let {
                            openUrlWithBrowser(it)
                        }
                    }
                }
            })

            setContentHashText(eip4Token.nftContentHash?.let { it.toHex() + " [SHA256]" }
                ?: texts.getString(STRING_LABEL_NONE))

            setThumbnail(
                if (uiLogic.downloadState != TokenInformationUiLogic.StateDownload.NOT_AVAILABLE) THUMBNAIL_TYPE_NONE
                else uiLogic.tokenInformation?.thumbnailType ?: 0
            )

            // show preview layout when download is running or when picture is ready
            updatePreview(uiLogic)
        }
    }

    fun updatePreview(uiLogic: TokenInformationUiLogic) {
        showPreview(uiLogic.downloadState, uiLogic.downloadPercent, uiLogic.downloadedData)
        showHashValidation(uiLogic.sha256Check)
    }

    abstract fun setContentLinkText(linkText: String)

    abstract fun setNftLayoutVisibility(visible: Boolean)

    abstract fun setContentLinkClickListener(listener: Runnable?)

    abstract fun setContentHashText(hashText: String)

    abstract fun setThumbnail(thumbnailType: Int)

    abstract fun showPreview(
        downloadState: TokenInformationUiLogic.StateDownload,
        downloadPercent: Float,
        content: ByteArray?
    )

    abstract fun showHashValidation(hashValid: Boolean?)
}
