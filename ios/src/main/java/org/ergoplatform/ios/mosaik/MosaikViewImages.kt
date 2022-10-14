package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.WIDTH_ICONS
import org.ergoplatform.mosaik.MosaikLogger
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSData
import org.robovm.apple.uikit.*

class ImageViewHolder(
    treeElement: TreeElement
) : UiViewHolder(UIImageView(), treeElement) {

    private val uiImageView = uiView as UIImageView
    private val activityIndicator: UIActivityIndicatorView?
    private val mosaikViewElement = treeElement.element as Image
    private val size = when (mosaikViewElement.size) {
        Image.Size.SMALL -> 50.0
        Image.Size.MEDIUM -> 120.0
        Image.Size.LARGE -> 250.0
        Image.Size.XXL -> 500.0
    }
    private var bytesProcessed: ByteArray? = null

    init {
        uiImageView.apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(size)
            fixedWidth(size)
        }

        val hasImage = treeElement.getResourceBytes?.let {
            resourceBytesAvailable(it)
            true
        } ?: false

        activityIndicator = if (!hasImage) {
            UIActivityIndicatorView(UIActivityIndicatorViewStyle.Medium).apply {
                uiView.addSubview(this)
                centerHorizontal().centerVertical()
                startAnimating()
            }
        } else null
    }

    override fun resourceBytesAvailable(bytes: ByteArray) {
        super.resourceBytesAvailable(bytes)
        // if we have an image that is not set yet
        if (bytes != bytesProcessed) viewCoroutineScope().launch(Dispatchers.IO) {
            val image = try {
                if (bytes.isNotEmpty())
                    UIImage(NSData(bytes)).scaleToSize(size, size)
                else null
            } catch (t: Throwable) {
                MosaikLogger.logDebug("Error decoding picture", t)
                null
            }

            runOnMainThread {
                if (image == null) {
                    uiImageView.image = getIosSystemImage(IMAGE_ERROR, UIImageSymbolScale.Small)
                    uiImageView.tintColor = uiColorErgo
                    uiImageView.contentMode = UIViewContentMode.Center
                } else {
                    uiImageView.image = image
                }
            }
        }
        bytesProcessed = bytes
        activityIndicator?.removeFromSuperview()
    }
}

class IconImageViewHolder(
    treeElement: TreeElement
) : UiViewHolder(UIImageView(), treeElement) {

    val uiImageView = uiView as UIImageView

    init {
        val mosaikViewElement = treeElement.element as Icon
        val size = when (mosaikViewElement.iconSize) {
            Icon.Size.SMALL -> WIDTH_ICONS * .75f
            Icon.Size.MEDIUM -> WIDTH_ICONS * 1.5f
            Icon.Size.LARGE -> WIDTH_ICONS * 3f
        }
        uiImageView.apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(size)
            fixedWidth(size)
            enforceKeepIntrinsicWidth()
            tintColor = when (mosaikViewElement.tintColor) {
                ForegroundColor.PRIMARY -> uiColorErgo
                ForegroundColor.DEFAULT -> UIColor.label()
                ForegroundColor.SECONDARY -> UIColor.secondaryLabel()
            }

            image = mosaikViewElement.iconType.getUiImage()
        }
    }
}

fun IconType.getUiImage() = getIosSystemImage(
    when (this) {
        IconType.INFO -> IMAGE_INFORMATION
        IconType.WARN -> IMAGE_WARNING
        IconType.ERROR -> IMAGE_ERROR
        IconType.CONFIG -> IMAGE_SETTINGS
        IconType.ADD -> IMAGE_PLUS
        IconType.EDIT -> IMAGE_EDIT_CIRCLE
        IconType.REFRESH -> IMAGE_RELOAD
        IconType.DELETE -> "xmark.circle"
        IconType.CROSS -> "xmark"
        IconType.WALLET -> IMAGE_WALLET
        IconType.SEND -> IMAGE_SEND
        IconType.RECEIVE -> IMAGE_RECEIVE
        IconType.MORE -> IMAGE_MORE_ACTION
        IconType.OPENLIST -> IMAGE_OPEN_LIST
        IconType.CHEVRON_UP -> IMAGE_CHEVRON_UP
        IconType.CHEVRON_DOWN -> IMAGE_CHEVRON_DOWN
        IconType.COPY -> "doc.on.doc"
        IconType.BACK -> IMAGE_CHEVRON_LEFT
        IconType.FORWARD -> "chevron.right"
        IconType.SWITCH -> "arrow.left.and.right.circle"
        IconType.QR_CODE -> IMAGE_QR_CODE
        IconType.QR_SCAN -> IMAGE_QR_SCAN
        IconType.SEARCH -> IMAGE_SEARCH
    },
    UIImageSymbolScale.Small
)

class LoadingIndicatorHolder(
    treeElement: TreeElement,
) : UiViewHolder(
    UIView(CGRect.Zero()),
    treeElement
) {

    private val indicatorView: UIActivityIndicatorView

    init {
        val indicatorElement = treeElement.element as LoadingIndicator

        val size = when (indicatorElement.size) {
            LoadingIndicator.Size.SMALL -> WIDTH_ICONS
            LoadingIndicator.Size.MEDIUM -> WIDTH_ICONS * 2
        }
        uiView.layoutMargins = UIEdgeInsets.Zero()

        indicatorView = UIActivityIndicatorView(
            when (indicatorElement.size) {
                LoadingIndicator.Size.SMALL -> UIActivityIndicatorViewStyle.Medium
                LoadingIndicator.Size.MEDIUM -> UIActivityIndicatorViewStyle.Large
            }
        )
        uiView.addSubview(indicatorView)
        indicatorView.centerHorizontal().centerVertical()
        uiView.fixedWidth(size).fixedHeight(size)
    }

    override fun onAddedToSuperview() {
        super.onAddedToSuperview()
        indicatorView.startAnimating()
    }
}

class QrCodeViewHolder(
    treeElement: TreeElement
) : UiViewHolder(UIImageView(), treeElement) {

    val uiImageView = uiView as UIImageView

    init {
        val mosaikViewElement = treeElement.element as QrCode
        val size = DEFAULT_QR_CODE_SIZE
        uiImageView.apply {
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedHeight(size)
            fixedWidth(size)

            setQrCode(mosaikViewElement.content, DEFAULT_QR_CODE_SIZE)
        }
    }
}

