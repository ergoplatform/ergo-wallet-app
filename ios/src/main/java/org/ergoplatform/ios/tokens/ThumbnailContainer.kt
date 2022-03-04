package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ThumbnailContainer(private val baseSize: Double = 24.0) : UIView(CGRect.Zero()) {
    private val thumbnailPicture = UIImageView(CGRect.Zero()).apply {
        contentMode = UIViewContentMode.ScaleAspectFit
        tintColor = UIColor.systemBackground()
        fixedWidth(baseSize / 2)
        fixedHeight(baseSize / 2)
    }

    init {
        val backGround = UIImageView(octagonImage.imageWithTintColor(UIColor.secondaryLabel())).apply {
            fixedWidth(baseSize)
            fixedHeight(
                baseSize
            )
            contentMode = UIViewContentMode.ScaleAspectFit
        }
        addSubview(backGround)
        backGround.edgesToSuperview()
        addSubview(thumbnailPicture)
        thumbnailPicture.centerVertical().centerHorizontallyTo(backGround)
        layoutMargins = UIEdgeInsets(0.0, 0.0, 0.0, DEFAULT_MARGIN)
    }

    fun setThumbnail(thumbnailType: Int) {
        val imageName = getTokenThumbnailImageName(thumbnailType)
        thumbnailPicture.image = imageName?.let {
            getIosSystemImage(it, UIImageSymbolScale.Small, 13.0)
        }
        isHidden = imageName == null
    }
}