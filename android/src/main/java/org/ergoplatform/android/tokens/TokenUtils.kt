package org.ergoplatform.android.tokens

import org.ergoplatform.ErgoAmount
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.android.R
import org.ergoplatform.ergoCurrencyText
import org.ergoplatform.persistance.*
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
    if (stateSyncManager.fiatCurrency.isNotEmpty()) {
        val tokenValueToShow =
            tokenErgoValueSum.toDouble() * stateSyncManager.fiatValue.value
        amount = tokenValueToShow
        setSymbol(stateSyncManager.fiatCurrency.uppercase())
    } else {
        setAmount(tokenErgoValueSum.toBigDecimal())
        setSymbol(ergoCurrencyText)
    }
}