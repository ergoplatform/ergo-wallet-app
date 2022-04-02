package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.*

fun getTokenGenuineImageName(genuineFlag: Int): String? {
    return when (genuineFlag) {
        GENUINE_VERIFIED -> IMAGE_VERIFIED
        GENUINE_SUSPICIOUS -> IMAGE_SUSPICIOUS
        else -> null
    }
}

fun getTokenThumbnailImageName(thumbnailType: Int): String? {
    return when (thumbnailType) {
        THUMBNAIL_TYPE_NFT_IMG -> IMAGE_PHOTO_CAMERA
        THUMBNAIL_TYPE_NFT_VID -> IMAGE_VIDEO_PLAY
        THUMBNAIL_TYPE_NFT_AUDIO -> IMAGE_MUSIC_NOTE
        else -> null
    }
}