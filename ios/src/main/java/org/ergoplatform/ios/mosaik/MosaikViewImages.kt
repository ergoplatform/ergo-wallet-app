package org.ergoplatform.ios.mosaik

import org.ergoplatform.ios.ui.fixedHeight
import org.ergoplatform.ios.ui.fixedWidth
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.Image
import org.robovm.apple.foundation.NSData
import org.robovm.apple.uikit.UIImage
import org.robovm.apple.uikit.UIImageView
import org.robovm.apple.uikit.UIViewContentMode

class ImageViewHolder(
    treeElement: TreeElement
): UiViewHolder(UIImageView(), treeElement) {

    val uiImageView = uiView as UIImageView

    init {
        val mosaikViewElement = treeElement.element as Image
        val size = when (mosaikViewElement.size) {
            Image.Size.SMALL -> 50.0
            Image.Size.MEDIUM -> 100.0
            Image.Size.LARGE -> 200.0
        }
        uiImageView.apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(size)
            fixedWidth(size)
        }

        treeElement.getResourceBytes?.let { resourceBytesAvailable(it) }
    }

    override fun resourceBytesAvailable(bytes: ByteArray) {
        super.resourceBytesAvailable(bytes)
        // if we have an image that is not set yet
        if (uiImageView.image == null) {
            try {
                uiImageView.image = UIImage(NSData(bytes))
            } catch (t: Throwable) {

            }
        }
    }
}