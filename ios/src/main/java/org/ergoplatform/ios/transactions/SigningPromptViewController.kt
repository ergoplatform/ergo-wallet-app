package org.ergoplatform.ios.transactions

import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.coldSigningRequestToQrChunks
import org.ergoplatform.transactions.getColdSignedTxChunk
import org.ergoplatform.uilogic.STRING_BUTTON_SCAN_SIGNED_TX
import org.ergoplatform.uilogic.STRING_ERROR_QR_PAGES_NUM
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIViewController

/**
 * SigningPromptViewController is shown when user makes a transaction on a read-only address, presenting QR code(s)
 * to scan with a cold wallet device.
 */
class SigningPromptViewController(
    private val signingPrompt: String,
    private val uiLogic: SendFundsUiLogic
) : UIViewController() {
    private lateinit var qrPresenter: PagedQrCodeContainer

    val texts = getAppDelegate().texts

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()

        qrPresenter = QrCodeContainer()

        view.addSubview(qrPresenter)
        qrPresenter.edgesToSuperview()

        addCloseButton()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        qrPresenter.rawData = signingPrompt
    }

    inner class QrCodeContainer : PagedQrCodeContainer(texts, texts.get(STRING_BUTTON_SCAN_SIGNED_TX)) {
        override fun calcChunksFromRawData(rawData: String, limit: Int): List<String> {
            return coldSigningRequestToQrChunks(
                rawData, limit
            )
        }

        override fun continueButtonPressed() {
            presentViewController(
                QrScannerViewController(dismissAnimated = false) { qrCode ->
                    getColdSignedTxChunk(qrCode)?.let {
                        if (it.pages > 1) {
                            // TODO cold wallet handle paged QR codes
                            val uac =
                                buildSimpleAlertController(
                                    "",
                                    texts.get(STRING_ERROR_QR_PAGES_NUM),
                                    getAppDelegate().texts
                                )
                            presentViewController(uac, false) {}
                        } else {
                            dismissViewController(false) {
                                val delegate = getAppDelegate()
                                uiLogic.sendColdWalletSignedTx(
                                    listOf(qrCode),
                                    delegate.prefs,
                                    IosStringProvider(delegate.texts)
                                )
                            }
                        }
                    }
                }, true
            ) {}
        }
    }
}