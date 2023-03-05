package org.ergoplatform.ios.ui

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.Main
import org.ergoplatform.ios.wallet.addresses.ChooseAddressListDialogViewController
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.STRING_BUTTON_COPY_SENSITIVE_DATA
import org.ergoplatform.uilogic.STRING_DESC_COPY_SENSITIVE_DATA
import org.ergoplatform.uilogic.STRING_LABEL_CANCEL
import org.ergoplatform.uilogic.STRING_ZXING_BUTTON_OK
import org.robovm.apple.coreanimation.CAFilter
import org.robovm.apple.coregraphics.CGPoint
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.coregraphics.CGSize
import org.robovm.apple.coreimage.CIContext
import org.robovm.apple.coreimage.CIFilter
import org.robovm.apple.foundation.*
import org.robovm.apple.uikit.*
import org.robovm.objc.Selector


const val MAX_WIDTH = 500.0
const val DEFAULT_MARGIN = 6.0
const val DEFAULT_TEXT_FIELD_HEIGHT = 40.0
const val DEFAULT_QR_CODE_SIZE = 280.0

const val IMAGE_WALLET = "rectangle.on.rectangle.angled"
val IMAGE_SETTINGS = if (Foundation.getMajorSystemVersion() >= 14) "gearshape" else "gear"
val IMAGE_TX_DONE = if (Foundation.getMajorSystemVersion() >= 15) "clock.badge.checkmark" else "checkmark.seal"
const val IMAGE_CREATE_WALLET = "folder.badge.plus"
const val IMAGE_RESTORE_WALLET = "arrow.clockwise"
const val IMAGE_RELOAD = "arrow.clockwise"
const val IMAGE_READONLY_WALLET = "magnifyingglass"
const val IMAGE_SEARCH = "magnifyingglass"
const val IMAGE_EXCLAMATION_MARK_FILLED = "exclamationmark.circle.fill"
const val IMAGE_NO_CONNECTION = "icloud.slash"
const val IMAGE_SEND = "paperplane"
const val IMAGE_RECEIVE = "arrow.down.left"
const val IMAGE_QR_CODE = "qrcode"
const val IMAGE_QR_SCAN = "qrcode.viewfinder"
const val IMAGE_PLUS_CIRCLE = "plus.circle.fill"
const val IMAGE_PLUS = "plus"
const val IMAGE_MINUS_CIRCLE = "minus.circle.fill"
const val IMAGE_REMOVE_TOKEN = "xmark"
const val IMAGE_FULL_AMOUNT = "arrow.down.circle"
const val IMAGE_MORE_ACTION = "ellipsis"
const val IMAGE_OPEN_LIST = "arrowtriangle.down.circle.fill"
val IMAGE_ADDRESS = if (Foundation.getMajorSystemVersion() >= 14) "arrow.triangle.branch" else "arrow.branch"
val IMAGE_ADDRESSBOOK = if (Foundation.getMajorSystemVersion() >= 15) "person.text.rectangle" else "person.crop.rectangle"
const val IMAGE_TRANSACTIONS = "arrow.right.arrow.left"
const val IMAGE_ADDRESS_LIST = "list.number"
const val IMAGE_CHEVRON_DOWN = "chevron.down"
const val IMAGE_CHEVRON_LEFT = "chevron.left"
const val IMAGE_CHEVRON_UP = "chevron.up"
val IMAGE_SWITCH_RESOLUTION = if (Foundation.getMajorSystemVersion() >= 14)
    "arrow.up.left.and.down.right.magnifyingglass" else "1.magnifyingglass"
const val IMAGE_WARNING = "exclamationmark.circle"
const val IMAGE_INFORMATION = "info.circle"
const val IMAGE_ERROR = "xmark.circle"
const val IMAGE_VERIFIED = "checkmark.seal.fill"
const val IMAGE_SUSPICIOUS = "exclamationmark.octagon.fill"
const val IMAGE_PHOTO_CAMERA = "camera.fill"
const val IMAGE_VIDEO_PLAY = "play.fill"
const val IMAGE_MUSIC_NOTE = "music.note"
const val IMAGE_OPEN_BROWSER = "arrow.up.right.square"
const val IMAGE_EDIT_CIRCLE = "pencil.circle"
const val IMAGE_AUTO_FIX = "wand.and.stars"
const val IMAGE_MOSAIK = "square.grid.2x2"
const val IMAGE_ARROW_RIGHT = "arrow.right"
const val IMAGE_STAR_OUTLINED = "star"
const val IMAGE_STAR_FILLED = "star.fill"
const val IMAGE_SHARE = "square.and.arrow.up"
const val IMAGE_PASTE = "doc.on.clipboard"
val IMAGE_RESET_DATA = if (Foundation.getMajorSystemVersion() >= 14) "leaf.arrow.triangle.circlepath" else "flame"
const val IMAGE_MULTISIG = "person.2.fill"

const val FONT_SIZE_BODY1 = 18.0
const val FONT_SIZE_HEADLINE1 = 30.0
const val FONT_SIZE_HEADLINE2 = 24.0
const val FONT_SIZE_TEXTBUTTON = 20.0

// See https://developer.apple.com/design/human-interface-guidelines/ios/visual-design/color/#system-colors
val uiColorErgo get() = UIColor.systemRed()
val ergoLogoImage get() = UIImage.getImage("ergologo")
val ergoLogoFilledImage get() = UIImage.getImage("ergologofilled")
val tokenLogoImage get() = UIImage.getImage("tokenlogo")
val octagonImage get() = UIImage.getImage("octagon")

fun getAppDelegate() = UIApplication.getSharedApplication().delegate as Main
fun runOnMainThread(r: Runnable) = NSOperationQueue.getMainQueue().addOperation(r)

@Suppress("DEPRECATION")
fun openUrlInBrowser(url: String) = UIApplication.getSharedApplication().openURL(NSURL(url))

fun openStorePage() = openUrlInBrowser("https://itunes.apple.com/app/id1643137927")

fun UIViewController.shareText(text: String, uiBarButtonItem: UIBarButtonItem) {
    shareText(text, uiBarButtonItem.keyValueCoder.getValue("view") as UIView)
}

fun UIViewController.shareText(text: String, sourceView: UIView) {
    val textShare = NSString(text)
    val texttoshare = NSArray(textShare)
    val share = UIActivityViewController(texttoshare, null)
    share.popoverPresentationController?.sourceView = sourceView
    presentViewController(share, true, null)
}

fun UIImageView.setQrCode(data: String, size: Double) {
    val nsString = NSString(data).toData(NSStringEncoding.ASCII)
    val filter = CIFilter("CIQRCodeGenerator")
    filter.keyValueCoder.setValue("inputMessage", nsString)
    val outputWithoutQuietZone = filter.outputImage

    outputWithoutQuietZone?.let { image ->
        val cgImage = CIContext().createCGImage(image, image.extent)
        val qrImage = UIImage(cgImage)
        val quietZonePixels = 5.0
        val widthWithQuietZone = qrImage.size.width + quietZonePixels * 2
        val sizeWithQuietZone = CGSize(widthWithQuietZone, widthWithQuietZone)

        UIGraphics.beginImageContext(sizeWithQuietZone, false, qrImage.scale)
        val imageWithZone = UIGraphics.getCurrentContext()?.let { ctx ->
            ctx.setFillColor(UIColor.white().cgColor)
            val container = CGRect(CGPoint.Zero(), sizeWithQuietZone)
            ctx.fillRect(container)
            qrImage.draw(CGRect(CGPoint(quietZonePixels, quietZonePixels), qrImage.size))
            UIGraphics.getImageFromCurrentImageContext()
        }
        UIGraphics.endImageContext()

        setImage(imageWithZone ?: qrImage)
    }
        layer.magnificationFilter = CAFilter.Nearest
        layer.setShouldRasterize(true)
}

fun UITextView.setHtmlText(html: String) {
    val attributedString = NSAttributedString(
        NSString(html.replace("\n", "<br>"))
            .toData(NSStringEncoding.Unicode),
        NSAttributedStringDocumentAttributes().apply {
            documentType = NSDocumentType.HTML
        })
    // TODO retain needed due to crashes on GC, see https://github.com/MobiVM/robovm/issues/608
    attributedString.retain()
    attributedText = attributedString
    dataDetectorTypes = UIDataDetectorTypes.Link
    isScrollEnabled = false
    isEditable = false
    textColor = UIColor.label()
    tintColor = uiColorErgo
}

/**
 * adds a trailing image in text. If text length exceeds image will get ellipsized
 */
fun UILabel.insertTrailingImage(image: UIImage) {
    val attachment = NSTextAttachment(image)
    val attributedString = NSAttributedString(attachment)
    val string = NSMutableAttributedString(text)
    string.append(attributedString)
    attributedText = string
}

fun UIView.wrapWithTrailingImage(
    image: UIImage,
    fixedWith: Double = 0.0,
    fixedHeight: Double = 0.0,
    keepWidth: Boolean = false, // determines if the right boundary is fixed or flexible
    inset: Double = DEFAULT_MARGIN * .7,
): TrailingImageView<UIView> {
    val imageView = UIImageView(image)
    imageView.tintColor = (this as? UILabel)?.textColor ?: this.tintColor

    val container = TrailingImageView(this, imageView)
    container.layoutMargins = UIEdgeInsets.Zero()
    if (fixedWith == 0.0) {
        imageView.enforceKeepIntrinsicWidth()
    } else {
        imageView.fixedWidth(fixedWith)
    }
    if (fixedHeight != 0.0) {
        imageView.fixedHeight(fixedHeight)
    }
    imageView.contentMode = UIViewContentMode.ScaleAspectFit
    imageView.centerVerticallyTo(this).leftToRightOf(this, inset).rightToSuperview(canBeLess = !keepWidth)
    this.leftToSuperview().topToSuperview().bottomToSuperview()
    return container
}

class TrailingImageView<T : UIView>(val content: T, val trailingImage: UIImageView) : UIView(CGRect.Zero()) {
    init {
        addSubview(content)
        addSubview(trailingImage)
    }
}

fun buildAddressSelectorView(
    vc: UIViewController,
    walletId: Int,
    showAllAddresses: Boolean,
    keepWidth: Boolean = false,
    addressChosen: (Int?) -> Unit
): TrailingImageView<UILabel> {
    val addressNameLabel = Body1BoldLabel().apply {
        numberOfLines = 1
        textColor = uiColorErgo
    }
    @Suppress("UNCHECKED_CAST")
    return addressNameLabel.wrapWithTrailingImage(
        getIosSystemImage(IMAGE_OPEN_LIST, UIImageSymbolScale.Small, 20.0)!!,
        keepWidth = keepWidth
    ).apply {
        isUserInteractionEnabled = true
        addGestureRecognizer(UITapGestureRecognizer {
            vc.presentViewController(
                ChooseAddressListDialogViewController(walletId, showAllAddresses, addressChosen),
                true
            ) {}
        })
    } as TrailingImageView<UILabel>
}

fun createTextview(): UITextView {
    val textView = UITextView(CGRect.Zero())
    textView.font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
    textView.layer.borderWidth = 1.0
    textView.layer.cornerRadius = 4.0
    textView.layer.borderColor = UIColor.systemGray().cgColor
    return textView
}

fun UITextView.setHasError(hasError: Boolean) {
    layer.borderColor = (if (hasError) UIColor.systemRed() else UIColor.systemGray()).cgColor
}

fun getIosSystemImage(name: String, scale: UIImageSymbolScale, pointSize: Double = 30.0): UIImage? {
    return UIImage.getSystemImage(
        name,
        UIImageSymbolConfiguration.getConfigurationWithPointSizeWeightScale(
            pointSize,
            UIImageSymbolWeight.Regular,
            scale
        )
    )
}

fun forceDismissKeyboard() {
    UIApplication.getSharedApplication()
        .sendAction(Selector.register("resignFirstResponder"), null, null, null)
}

fun UIStackView.clearArrangedSubviews() {
    arrangedSubviews.toMutableList().forEach {
        removeArrangedSubview(it)
        it.removeFromSuperview()
    }
}

/**
 * Enforce to keep the intrinsic width by not growing or shrinking this view.
 */
fun UIView.enforceKeepIntrinsicWidth() {
    // default resistance is 750 - setting it higher means this view resists more than others
    setContentCompressionResistancePriority(1000f, UILayoutConstraintAxis.Horizontal)
    // default 250 - setting it higher means this view hugs more than others
    setContentHuggingPriority(700f, UILayoutConstraintAxis.Horizontal)
}

/**
 * adds a close button to the top left corner of this UIViewController. If no action is given,
 * it will dismiss the view controller.
 */
fun UIViewController.addCloseButton(action: Runnable? = null): UIButton {
    val closeButton = UIButton(UIButtonType.Close)
    view.addSubview(closeButton)
    closeButton.addOnTouchUpInsideListener { _, _ ->
        if (action != null) action.run() else dismissViewController(true) {}
    }
    closeButton.topToSuperview(topInset = DEFAULT_MARGIN).leftToSuperview()

    return closeButton
}

fun UIView.animateLayoutChanges(block: Runnable) {
    UIView.transition(this, 0.3, UIViewAnimationOptions.TransitionCrossDissolve, block) {}
}

fun UIView.setHiddenAnimated(hidden: Boolean) {
    if (hidden != isHidden) {
        superview.animateLayoutChanges { isHidden = hidden }
    }
}

/**
 * builds a simple alert controller (or message box) with a title, message and OK button
 */
fun buildSimpleAlertController(title: String, message: String, texts: I18NBundle): UIAlertController {
    val uac = UIAlertController(title, message, UIAlertControllerStyle.Alert)
    uac.addAction(
        UIAlertAction(
            texts.get(STRING_ZXING_BUTTON_OK),
            UIAlertActionStyle.Default
        ) {})
    return uac
}

fun buildSensitiveDataCopyDialog(texts: I18NBundle, dataToCopy: String): UIAlertController {
    val uac = UIAlertController("", texts.get(STRING_DESC_COPY_SENSITIVE_DATA), UIAlertControllerStyle.Alert)
    uac.addAction(
        UIAlertAction(
            texts.get(STRING_BUTTON_COPY_SENSITIVE_DATA),
            UIAlertActionStyle.Default
        ) {
            UIPasteboard.getGeneralPasteboard()?.string = dataToCopy
        })
    uac.addAction(
        UIAlertAction(
            texts.get(STRING_LABEL_CANCEL),
            UIAlertActionStyle.Cancel
        ) {})
    return uac
}

var UIScrollView.page
    get() = (contentOffset.x / frame.size.width).toInt()
    set(value) {
        setContentOffset(CGPoint(frame.size.width * value.toDouble(), contentOffset.y), true)
    }

fun UIScrollView.scrollToBottom(animated: Boolean = true) {
    setContentOffset(
        CGPoint(0.0, contentSize.height - bounds.size.height + contentInset.bottom),
        animated
    )
}

fun UIScrollView.scrollToTop(animated: Boolean = true) {
    setContentOffset(CGPoint(0.0, 0.0), animated)
}

fun UIImage.scaleToSize(scaleWidth: Double, scaleHeight: Double): UIImage? {
    val scaledImageSize = CGSize(scaleWidth, scaleHeight)
    val renderer = UIGraphicsImageRenderer(scaledImageSize)
    return renderer.toImage {
        this.draw(CGRect(CGPoint.Zero(), scaledImageSize))
    }
}

fun MessageSeverity.getImage(): String? {
    return when (this) {
        MessageSeverity.NONE -> null
        MessageSeverity.INFORMATION -> IMAGE_INFORMATION
        MessageSeverity.WARNING -> IMAGE_WARNING
        MessageSeverity.ERROR -> IMAGE_ERROR
    }
}