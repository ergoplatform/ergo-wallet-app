package org.ergoplatform.ios.ui

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.Main
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGAffineTransform
import org.robovm.apple.coregraphics.CGPoint
import org.robovm.apple.coregraphics.CGRect
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
const val IMAGE_READONLY_WALLET = "magnifyingglass"
const val IMAGE_EXCLAMATION_MARK_FILLED = "exclamationmark.circle.fill"
const val IMAGE_NO_CONNECTION = "icloud.slash"
const val IMAGE_SEND = "paperplane"
const val IMAGE_RECEIVE = "arrow.down.left"
const val IMAGE_QR_CODE = "qrcode"
const val IMAGE_QR_SCAN = "qrcode.viewfinder"
const val IMAGE_PLUS_CIRCLE = "plus.circle.fill"
const val IMAGE_PLUS = "plus"
const val IMAGE_MINUS_CIRCLE = "minus.circle.fill"
const val IMAGE_CROSS_CIRCLE = "xmark.circle"
const val IMAGE_FULL_AMOUNT = "arrow.down.circle"
const val IMAGE_MORE_ACTION = "ellipsis"
const val IMAGE_OPEN_LIST = "arrowtriangle.down.circle.fill"
val IMAGE_ADDRESS = if (Foundation.getMajorSystemVersion() >= 14) "arrow.triangle.branch" else "arrow.branch"
const val IMAGE_TRANSACTIONS = "arrow.right.arrow.left"
const val IMAGE_ADDRESS_LIST = "list.number"
const val IMAGE_CHEVRON_DOWN = "chevron.down"
const val IMAGE_CHEVRON_UP = "chevron.up"
val IMAGE_SWITCH_RESOLUTION = if (Foundation.getMajorSystemVersion() >= 14)
    "arrow.up.left.and.down.right.magnifyingglass" else "1.magnifyingglass"
const val IMAGE_WARNING = "exclamationmark.circle"
const val IMAGE_INFORMATION = "info.circle"
const val IMAGE_ERROR = "xmark.circle"

const val FONT_SIZE_BODY1 = 18.0
const val FONT_SIZE_HEADLINE1 = 30.0
const val FONT_SIZE_HEADLINE2 = 24.0
const val FONT_SIZE_TEXTBUTTON = 20.0

// See https://developer.apple.com/design/human-interface-guidelines/ios/visual-design/color/#system-colors
val uiColorErgo get() = UIColor.systemRed()
val ergoLogoImage get() = UIImage.getImage("ergologo")
val ergoLogoFilledImage get() = UIImage.getImage("ergologofilled")
val tokenLogoImage get() = UIImage.getImage("tokenlogo")

fun getAppDelegate() = UIApplication.getSharedApplication().delegate as Main
fun runOnMainThread(r: Runnable) = NSOperationQueue.getMainQueue().addOperation(r)

@Suppress("DEPRECATION")
fun openUrlInBrowser(url: String) = UIApplication.getSharedApplication().openURL(NSURL(url))

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
    val unscaledOutput = filter.outputImage

    unscaledOutput?.let {
        val scaleX = size / it.extent.size.width
        val scaleY = size / it.extent.size.height
        val transform = CGAffineTransform.createScale(scaleX, scaleY)

        val output = unscaledOutput.newImageByApplyingTransform(transform)
        val image = UIImage(output)
        setImage(image)
    }
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

fun UIView.wrapWithTrailingImage(image: UIImage, fixedWith: Double = 0.0, fixedHeight: Double = 0.0): UIView {
    val imageView = UIImageView(image)
    imageView.tintColor = (this as? UILabel)?.textColor ?: this.tintColor

    val container = UIView(CGRect.Zero())
    container.layoutMargins = UIEdgeInsets.Zero()
    container.addSubview(this)
    container.addSubview(imageView)
    if (fixedWith == 0.0) {
        imageView.enforceKeepIntrinsicWidth()
    } else {
        imageView.fixedWidth(fixedWith)
    }
    if (fixedHeight != 0.0) {
        imageView.fixedHeight(fixedHeight)
    }
    imageView.contentMode = UIViewContentMode.ScaleAspectFit
    imageView.centerVerticallyTo(this).leftToRightOf(this, DEFAULT_MARGIN * .7).rightToSuperview(canBeLess = true)
    this.leftToSuperview().topToSuperview().bottomToSuperview()
    return container
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

fun createTextField(): UITextField {
    val textField = UITextField(CGRect.Zero())
    textField.font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
    textField.layer.borderWidth = 1.0
    textField.layer.cornerRadius = 4.0
    textField.layer.borderColor = UIColor.systemGray().cgColor
    val padding = UIView(CGRect(0.0, 0.0, 5.0, 10.0))
    textField.leftView = padding
    textField.leftViewMode = UITextFieldViewMode.Always
    textField.fixedHeight(DEFAULT_TEXT_FIELD_HEIGHT)
    return textField
}

fun UITextField.setHasError(hasError: Boolean) {
    layer.borderColor = (if (hasError) UIColor.systemRed() else UIColor.systemGray()).cgColor
    if (hasError) {
        val errorView = prepareTextFieldImageContainer(
            getIosSystemImage(IMAGE_EXCLAMATION_MARK_FILLED, UIImageSymbolScale.Small)!!,
            UIColor.systemRed()
        )
        rightView = errorView
        rightViewMode = UITextFieldViewMode.Always
    } else {
        rightView = null
    }
}

private fun prepareTextFieldImageContainer(image: UIImage, tintColor: UIColor = UIColor.label()): UIView {
    val customIcon = UIImageView(image)
    customIcon.tintColor = tintColor
    customIcon.contentMode = UIViewContentMode.Center
    val iconContainer = UIView(CGRect(0.0, 0.0, 35.0, 30.0))
    iconContainer.addSubview(customIcon)
    return iconContainer
}

fun UITextField.setCustomActionField(image: UIImage, action: Runnable) {
    val iconContainer = prepareTextFieldImageContainer(image)
    rightView = iconContainer
    rightViewMode = UITextFieldViewMode.Always
    iconContainer.isUserInteractionEnabled = true
    iconContainer.addGestureRecognizer(UITapGestureRecognizer {
        action.run()
    })
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