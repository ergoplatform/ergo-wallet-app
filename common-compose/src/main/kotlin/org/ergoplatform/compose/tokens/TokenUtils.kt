package org.ergoplatform.compose.tokens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import org.ergoplatform.compose.ComposePlatformUtils
import org.ergoplatform.persistance.*

fun getGenuineImageVector(genuineFlag: Int): ImageVector? {
    return when (genuineFlag) {
        GENUINE_VERIFIED -> Icons.Default.Verified
        GENUINE_SUSPICIOUS -> Icons.Default.Report
        else -> null
    }
}

fun getThumbnailDrawableId(thumbnailType: Int): ComposePlatformUtils.Drawable? {
    return when (thumbnailType) {
        THUMBNAIL_TYPE_NFT_IMG -> ComposePlatformUtils.Drawable.NftImage
        THUMBNAIL_TYPE_NFT_VID -> ComposePlatformUtils.Drawable.NftVideo
        THUMBNAIL_TYPE_NFT_AUDIO -> ComposePlatformUtils.Drawable.NftAudio
        else -> null
    }
}