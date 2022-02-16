package org.ergoplatform.ios.ui

import org.ergoplatform.uilogic.STRING_BUTTON_COPY
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ShareWithQrCodeViewController(private val dataToShare: String) : UIViewController() {

    override fun viewDidLoad() {
        super.viewDidLoad()

        val texts = getAppDelegate().texts

        view.backgroundColor = UIColor.systemBackground()

        val closeButton = addCloseButton {
            dismissViewController(true) {}
        }

        val qrCode = UIImageView(CGRect.Zero()).apply {
            contentMode = UIViewContentMode.ScaleAspectFit
        }
        val qrCodeContainer = UIView()
        qrCodeContainer.addSubview(qrCode)
        qrCode.fixedWidth(DEFAULT_QR_CODE_SIZE).fixedHeight(DEFAULT_QR_CODE_SIZE).centerHorizontal()
            .topToSuperview().bottomToSuperview()
        qrCode.setQrCode(dataToShare, DEFAULT_QR_CODE_SIZE)

        val dataToShareLabel = Body2BoldLabel().apply {
            text = dataToShare
        }

        val buttonContainer = UIView(CGRect.Zero())
        val copyButton = TextButton(texts.get(STRING_BUTTON_COPY)).apply {
            addOnTouchUpInsideListener { _, _ ->
                UIPasteboard.getGeneralPasteboard()?.string = dataToShare
            }
        }
        buttonContainer.addSubview(copyButton)
        copyButton.topToSuperview().bottomToSuperview().rightToSuperview()

        val stackview =
            UIStackView(
                NSArray(
                    qrCodeContainer,
                    dataToShareLabel,
                    buttonContainer
                )
            ).apply {
                axis = UILayoutConstraintAxis.Vertical
                layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, 0.0, 0.0, 0.0)
                isLayoutMarginsRelativeArrangement = true
                setCustomSpacing(DEFAULT_MARGIN * 2, qrCodeContainer)
                setCustomSpacing(DEFAULT_MARGIN * 2, dataToShareLabel)
            }

        val scrollView = stackview.wrapInVerticalScrollView()
        scrollView.setDelaysContentTouches(false)

        view.addSubview(scrollView)
        scrollView.widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview().topToBottomOf(closeButton, DEFAULT_MARGIN)
    }
}