package org.ergoplatform.ios.tokens

import org.ergoplatform.ios.ui.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*


class GenuineImageContainer : UIView(CGRect.Zero()) {
    private val imageView = UIImageView(CGRect.Zero()).apply {
        tintColor = uiColorErgo
        contentMode = UIViewContentMode.ScaleAspectFit
        fixedWidth(20.0)
    }

    init {
        addSubview(imageView)
        imageView.edgesToSuperview()
        layoutMargins = UIEdgeInsets(0.0, DEFAULT_MARGIN, 0.0, 0.0)

    }

    fun setGenuineFlag(genuineFlag: Int) {
        val imageName = getTokenGenuineImageName(genuineFlag)
        imageView.image = imageName?.let { getIosSystemImage(it, UIImageSymbolScale.Small, 20.0) }
        isHidden = imageName == null
    }
}