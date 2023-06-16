package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.IMAGE_FILTER
import org.ergoplatform.ios.ui.getAppDelegate
import org.ergoplatform.ios.ui.getIosSystemImage
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NFT_AUDIO
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NFT_IMG
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NFT_VID
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NONE
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_AUDIO
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_GENERIC
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_IMAGE
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_VIDEO
import org.ergoplatform.uilogic.tokens.FilterTokenListUiLogic
import org.robovm.apple.foundation.Foundation
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.UIAction
import org.robovm.apple.uikit.UIButton
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIControlState
import org.robovm.apple.uikit.UIImageSymbolScale
import org.robovm.apple.uikit.UIMenu
import org.robovm.apple.uikit.UIMenuElementState

class TokenFilterButton(val uiLogic: FilterTokenListUiLogic) : UIButton() {
    var onChange: (() -> Unit)? = null

    init {
        setImage(
            getIosSystemImage(IMAGE_FILTER, UIImageSymbolScale.Small, pointSize = 25.0),
            UIControlState.Normal
        )
        tintColor = UIColor.label()
        if (Foundation.getMajorSystemVersion() >= 14) {
            setShowsMenuAsPrimaryAction(true)
            buildMenu()
        } else {
            isHidden = true
        }
    }

    private fun buildMenu() {
        val texts = getAppDelegate().texts
        val genericAction =
            buildAction(texts.get(STRING_LABEL_TOKEN_GENERIC), THUMBNAIL_TYPE_NONE)
        val imageAction = buildAction(
            texts.get(STRING_LABEL_TOKEN_IMAGE),
            THUMBNAIL_TYPE_NFT_IMG,
        )
        val audioAction = buildAction(
            texts.get(STRING_LABEL_TOKEN_AUDIO),
            THUMBNAIL_TYPE_NFT_AUDIO,
        )
        val videoAction = buildAction(
            texts.get(STRING_LABEL_TOKEN_VIDEO),
            THUMBNAIL_TYPE_NFT_VID,
        )
        menu = UIMenu(NSArray(genericAction, imageAction, audioAction, videoAction))
    }

    private fun buildAction(text: String, tokenType: Int): UIAction =
        UIAction(text,
            getTokenThumbnailImageName(tokenType)?.let {
                getIosSystemImage(it, UIImageSymbolScale.Small)
            }, null
        ) { toggleToken(tokenType) }.apply {
            state = if (uiLogic.hasTokenFilter(tokenType))
                UIMenuElementState.On
            else UIMenuElementState.Off
        }

    private fun toggleToken(type: Int) {
        uiLogic.toggleTokenFilter(type)
        buildMenu()
        onChange?.invoke()
    }

    override fun setHidden(v: Boolean) {
        super.setHidden(
            if (Foundation.getMajorSystemVersion() >= 14) v else true
        )
    }
}