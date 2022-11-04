package org.ergoplatform.ios.transactions

import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_LABEL_QR_PAGES_INFO
import org.ergoplatform.uilogic.transactions.SigningPromptDialogDataSource
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIScrollView
import org.robovm.apple.uikit.UIView
import org.robovm.apple.uikit.UIViewController

/**
 * SigningPromptViewController is shown when user makes a transaction on a read-only address, presenting QR code(s)
 * to scan with a cold wallet device.
 */
class SigningPromptViewController(
    private val dataSource: SigningPromptDialogDataSource,
    private val onSigningPromptResponseScanComplete: () -> Unit,
) : UIViewController() {
    private lateinit var qrPresenter: PagedQrCodeContainer

    private val texts = getAppDelegate().texts
    private val scannedPagesLabel = Headline2Label()
    private lateinit var scrollView: UIScrollView

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()

        qrPresenter = QrCodeContainer()

        val scrollingContainer = UIView(CGRect.Zero())
        scrollView = scrollingContainer.wrapInVerticalScrollView()

        scrollingContainer.addSubview(qrPresenter)
        scrollingContainer.addSubview(scannedPagesLabel)
        qrPresenter.topToSuperview().widthMatchesSuperview()
        scannedPagesLabel.topToBottomOf(qrPresenter).bottomToSuperview().centerHorizontal()

        view.addSubview(scrollView)
        scrollView.edgesToSuperview()
        addCloseButton()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        qrPresenter.rawData = dataSource.signingPromptData
    }

    fun refreshPagesInfo() {
        scannedPagesLabel.text = dataSource.responsePagesCollector?.let {
            if (it.pagesAdded > 0) texts.format(
                STRING_LABEL_QR_PAGES_INFO,
                it.pagesAdded, it.pagesCount
            ) else ""
        } ?: ""
    }

    inner class QrCodeContainer :
        PagedQrCodeContainer(
            texts,
            dataSource.lastPageButtonLabel,
            lastPageDescriptionLabel = dataSource.lastPageDescriptionLabel,
            descriptionLabel = dataSource.descriptionLabel
        ) {
        override fun calcChunksFromRawData(rawData: String, limit: Int): List<String> {
            return dataSource.signingRequestToQrChunks(rawData, limit)
        }

        override fun continueButtonPressed() {
            presentViewController(
                QrScannerViewController(dismissAnimated = false) { qrCode ->
                    dataSource.responsePagesCollector?.let {
                        it.addPage(qrCode)
                        if (it.hasAllPages()) {
                            dismissViewController(false) {
                                onSigningPromptResponseScanComplete()
                            }
                        } else {
                            refreshPagesInfo()
                            scrollView.layoutIfNeeded()
                            scrollView.scrollToBottom()
                        }
                    }
                }, true
            ) {}
        }
    }
}