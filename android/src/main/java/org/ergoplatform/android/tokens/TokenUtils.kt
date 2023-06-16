package org.ergoplatform.android.tokens

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.forEach
import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.android.R
import org.ergoplatform.ergoCurrencyText
import org.ergoplatform.persistance.*
import org.ergoplatform.uilogic.tokens.FilterTokenListUiLogic
import org.fabiomsr.moneytextview.MoneyTextView

fun TokenInformation.getGenuineDrawableId() = getGenuineDrawableId(genuineFlag)

fun getGenuineDrawableId(genuineFlag: Int): Int {
    return when (genuineFlag) {
        GENUINE_VERIFIED -> R.drawable.ic_verified_18
        GENUINE_SUSPICIOUS -> R.drawable.ic_suspicious_18
        else -> 0
    }
}

fun TokenInformation.getThumbnailDrawableId() = getThumbnailDrawableId(thumbnailType)

fun getThumbnailDrawableId(thumbnailType: Int): Int {
    return when (thumbnailType) {
        THUMBNAIL_TYPE_NFT_IMG -> R.drawable.ic_photo_camera_24
        THUMBNAIL_TYPE_NFT_VID -> R.drawable.ic_videocam_24
        THUMBNAIL_TYPE_NFT_AUDIO -> R.drawable.ic_music_note_24
        else -> 0
    }
}

fun MoneyTextView.setTokenPrice(
    tokenErgoValueSum: ErgoAmount,
    stateSyncManager: WalletStateSyncManager
) {
    if (stateSyncManager.hasFiatValue) {
        val tokenValueToShow =
            tokenErgoValueSum.toDouble() * stateSyncManager.fiatValue.value
        amount = tokenValueToShow
        setSymbol(stateSyncManager.fiatCurrency.uppercase())
    } else {
        setAmount(tokenErgoValueSum.toBigDecimal())
        setSymbol(ergoCurrencyText)
    }
}

fun showTokenFilterPopup(
    uiLogic: FilterTokenListUiLogic,
    context: Context,
    view: View,
    onClick: (() -> Unit)? = null,
) {
    val popupMenu = PopupMenu(context, view)

    popupMenu.menuInflater.inflate(R.menu.filter_tokens, popupMenu.menu)

    popupMenu.menu.forEach {
        it.isChecked = uiLogic.hasTokenFilter(
            when (it.itemId) {
                R.id.token_type_image -> THUMBNAIL_TYPE_NFT_IMG
                R.id.token_type_audio -> THUMBNAIL_TYPE_NFT_AUDIO
                R.id.token_type_video -> THUMBNAIL_TYPE_NFT_VID
                else -> THUMBNAIL_TYPE_NONE
            }
        )
    }

    popupMenu.setOnMenuItemClickListener {
        uiLogic.toggleTokenFilter(
            when (it.itemId) {
                R.id.token_type_image -> THUMBNAIL_TYPE_NFT_IMG
                R.id.token_type_audio -> THUMBNAIL_TYPE_NFT_AUDIO
                R.id.token_type_video -> THUMBNAIL_TYPE_NFT_VID
                else -> THUMBNAIL_TYPE_NONE
            }
        )
        onClick?.invoke()
        true
    }

    // Showing the popup menu
    popupMenu.show()
}