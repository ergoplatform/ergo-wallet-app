package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.CrashHandler
import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.MosaikGuidManager
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.actions.ErgoAuthAction
import org.ergoplatform.mosaik.model.actions.ErgoPayAction
import org.ergoplatform.uilogic.STRING_BUTTON_BACK
import org.robovm.apple.uikit.*

class MosaikViewController(
    private val appUrl: String,
    private val appName: String?,
) : ViewControllerWithKeyboardLayoutGuide() {

    override fun viewDidLoad() {
        super.viewDidLoad()

        title = appName ?: ""

        val appDelegate = getAppDelegate()
        mosaikRuntime.apply {
            appDatabase = appDelegate.database
            guidManager.appDatabase = appDatabase
            // TODO cacheFileManager =
            stringProvider = IosStringProvider(appDelegate.texts)
            preferencesProvider = appDelegate.prefs

            loadUrlEnteredByUser(this@MosaikViewController.appUrl)
        }

        navigationItem.setHidesBackButton(true)
        val backButton = UIBarButtonItem(appDelegate.texts.get(STRING_BUTTON_BACK), UIBarButtonItemStyle.Plain)
        navigationItem.leftBarButtonItem = backButton
        backButton.setOnClickListener {
            if (mosaikRuntime.canNavigateBack())
                mosaikRuntime.navigateBack()
            else
                navigationController.popViewController(true)
        }
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        onResume()
    }

    override fun onResume() {
        super.onResume()
        mosaikRuntime.checkViewTreeValidity()
    }

    private val mosaikRuntime = object : AppMosaikRuntime(
        "Ergo Wallet App (iOS)",
        CrashHandler.getAppVersion() ?: "",
        { org.ergoplatform.mosaik.model.MosaikContext.Platform.PHONE }, // TODO iPad
        MosaikGuidManager().apply {
            appDatabase = org.ergoplatform.ios.ui.getAppDelegate().database
        }
    ) {
        override fun onAppNavigated(manifest: MosaikManifest) {
            runOnMainThread {
                title = manifest.appName
                // TODO favorite button
            }
        }

        override fun appNotLoaded(cause: Throwable) {
            TODO("Not yet implemented")
        }

        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun onAddressLongPress(address: String) {
            pasteToClipboard(address)
        }

        override fun openBrowser(url: String) {
            openUrlInBrowser(url)
        }

        override fun pasteToClipboard(text: String) {
            pasteToClipboard(text)
        }

        override fun runErgoAuthAction(action: ErgoAuthAction) {
            TODO("Not yet implemented")
        }

        override fun runErgoPayAction(action: ErgoPayAction) {
            TODO("Not yet implemented")
        }

        override fun runTokenInformationAction(tokenId: String) {
            TODO("Not yet implemented")
        }

        override fun scanQrCode(actionId: String) {
            TODO("Not yet implemented")
        }

        override fun showDialog(dialog: MosaikDialog) {
            val uac = UIAlertController("", dialog.message, UIAlertControllerStyle.Alert)
            uac.addAction(
                UIAlertAction(
                    dialog.positiveButtonText,
                    UIAlertActionStyle.Default
                ) {
                    dialog.positiveButtonClicked?.invoke()
                })

            dialog.negativeButtonText?.let { buttonText ->
                uac.addAction(
                    UIAlertAction(
                        dialog.negativeButtonText,
                        UIAlertActionStyle.Default
                    ) {
                        dialog.negativeButtonClicked?.invoke()
                    }
                )
            }

            runOnMainThread {
                presentViewController(uac, true) {}
            }
        }

        override fun showErgoAddressChooser(valueId: String) {
            TODO("Not yet implemented")
        }

        override fun showErgoWalletChooser(valueId: String) {
            TODO("Not yet implemented")
        }

    }
}