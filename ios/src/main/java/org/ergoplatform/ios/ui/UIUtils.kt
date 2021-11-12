package org.ergoplatform.ios.ui

import org.ergoplatform.ios.Main
import org.robovm.apple.coregraphics.CGAffineTransform
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.coreimage.CIFilter
import org.robovm.apple.foundation.*
import org.robovm.apple.uikit.*


const val MAX_WIDTH = 500.0
const val DEFAULT_MARGIN = 6.0

const val IMAGE_WALLET = "rectangle.on.rectangle.angled"
val IMAGE_SETTINGS = if (Foundation.getMajorSystemVersion() >= 14) "gearshape" else "gear"
const val IMAGE_CREATE_WALLET = "folder.badge.plus"
const val IMAGE_RESTORE_WALLET = "arrow.clockwise"
const val IMAGE_READONLY_WALLET = "magnifyingglass"

const val FONT_SIZE_BODY1 = 18.0

// See https://developer.apple.com/design/human-interface-guidelines/ios/visual-design/color/#system-colors
val uiColorErgo get() = UIColor.systemRed()
val ergoLogoImage get() = UIImage.getImage("ergologo")

fun getAppDelegate() = UIApplication.getSharedApplication().delegate as Main
fun runOnMainThread(r: Runnable) = NSOperationQueue.getMainQueue().addOperation(r)
fun openUrlInBrowser(url: String) = UIApplication.getSharedApplication().openURL(NSURL(url))

fun UIViewController.shareText(text: String, sourceView: UIView) {
    val textShare = NSString(text)
    val texttoshare = NSArray(textShare)
    val share = UIActivityViewController(texttoshare, null)
    share.popoverPresentationController?.sourceView = sourceView
    presentViewController(share, true, null)
}

fun UIImageView.setQrCode(data: String, size: Int) {
    val nsString = NSString(data).toData(NSStringEncoding.ASCII)
    val filter = CIFilter("CIQRCodeGenerator")
    filter?.let {
        it.keyValueCoder.setValue("inputMessage", nsString)
        val transform = CGAffineTransform.Identity()
        transform.scale(3.0, 3.0)

        val output = it.outputImage?.newImageByApplyingTransform(transform)
        output?.let {
            val image = UIImage(output)
            setImage(image)
            contentMode = UIViewContentMode.ScaleAspectFit
            backgroundColor = UIColor.systemRed()

        }
    }
}

fun UITextView.setHtmlText(html: String) {
    val attributedString = NSAttributedString(
        NSString(html.replace("\n", "<br>"))
            .toData(NSStringEncoding.Unicode),
        NSAttributedStringDocumentAttributes().apply {
            documentType = NSDocumentType.HTML
        })

    attributedText = attributedString
    dataDetectorTypes = UIDataDetectorTypes.Link
    //isSelectable = true
    isScrollEnabled = false
    isEditable = false
}

fun createTextview(): UITextView {
    val textView = UITextView(CGRect.Zero())
    textView.font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
    textView.layer.borderWidth = 1.0
    textView.layer.borderColor = UIColor.systemGray().cgColor
    return textView
}