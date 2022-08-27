package org.ergoplatform.android.mosaik

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.BuildConfig
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.ergoauth.isErgoAuthRequest
import org.ergoplatform.mosaik.AppMosaikRuntime
import org.ergoplatform.mosaik.MosaikDialog
import org.ergoplatform.mosaik.MosaikGuidManager
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.actions.ErgoAuthAction
import org.ergoplatform.mosaik.model.actions.ErgoPayAction
import org.ergoplatform.mosaik.model.actions.TokenInformationAction
import org.ergoplatform.persistance.CacheFileManager
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.transactions.isErgoPaySigningRequest
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.wallet.getDerivedAddress
import org.ergoplatform.wallet.getSortedDerivedAddressesList

class MosaikViewModel : ViewModel() {
    val browserEvent = SingleLiveEvent<String?>()
    val pasteToClipboardEvent = SingleLiveEvent<String?>()
    val showDialogEvent = SingleLiveEvent<MosaikDialog?>()
    val showWalletOrAddressChooserEvent = SingleLiveEvent<String?>()
    val ergoPayActionEvent = SingleLiveEvent<ErgoPayAction?>()
    val ergoAuthActionEvent = SingleLiveEvent<ErgoAuthAction?>()
    val scanQrCodeEvent = SingleLiveEvent<String?>()
    val showTokenInfoEvent = SingleLiveEvent<String?>()
    val manifestLiveData = MutableLiveData<MosaikManifest?>()
    val noAppLiveData = MutableLiveData<Throwable?>()

    private var initialized = false

    private var valueIdForWalletChooser: String? = null
    private var valueIdForAddressChooser: String? = null
    private var walletForAddressChooser: Wallet? = null
    private var ergoPayOnFinishedActionId: String? = null
    private var ergoAuthOnFinishedActionId: String? = null
    private var scanQrCodeActionId: String? = null

    val mosaikRuntime = object : AppMosaikRuntime(
        "Ergo Wallet App (Android)",
        BuildConfig.VERSION_NAME,
        { platformType!! },
        MosaikGuidManager(),
    ) {
        override val coroutineScope: CoroutineScope
            get() = viewModelScope

        override fun openBrowser(url: String) {
            browserEvent.postValue(url)
        }

        override fun pasteToClipboard(text: String) {
            pasteToClipboardEvent.postValue(text)
        }

        override fun onAddressLongPress(address: String) {
            pasteToClipboard(address)
        }

        override fun showDialog(dialog: MosaikDialog) {
            showDialogEvent.postValue(dialog)
        }

        override fun onAppNavigated(manifest: MosaikManifest) {
            manifestLiveData.postValue(manifest)
        }

        override fun appNotLoaded(cause: Throwable) {
            noAppLiveData.postValue(cause)
        }

        override fun runErgoPayAction(action: ErgoPayAction) {
            if (isErgoPaySigningRequest(action.url)) {
                ergoPayOnFinishedActionId = action.onFinished
                ergoPayActionEvent.postValue(action)
            } else {
                raiseError(IllegalArgumentException("ErgoPayAction without actual signing request: ${action.url}"))
            }
        }

        override fun runErgoAuthAction(action: ErgoAuthAction) {
            if (isErgoAuthRequest(action.url)) {
                ergoAuthOnFinishedActionId = action.onFinished
                ergoAuthActionEvent.postValue(action)
            } else {
                raiseError(IllegalArgumentException("ErgoAuthAction without actual authentication request: ${action.url}"))
            }
        }

        override fun scanQrCode(actionId: String) {
            scanQrCodeActionId = actionId
            scanQrCodeEvent.postValue(actionId)
        }

        override fun showErgoAddressChooser(valueId: String) {
            valueIdForAddressChooser = valueId
            valueIdForWalletChooser = null
            showWalletOrAddressChooserEvent.postValue(valueId)
        }

        override fun showErgoWalletChooser(valueId: String) {
            valueIdForWalletChooser = valueId
            valueIdForAddressChooser = null
            showWalletOrAddressChooserEvent.postValue(valueId)
        }

        override fun runTokenInformationAction(tokenId: String) {
            showTokenInfoEvent.postValue(tokenId)
        }
    }

    var platformType: MosaikContext.Platform? = null

    fun initialize(
        appUrl: String,
        appDb: AppDatabase,
        platformType: MosaikContext.Platform,
        stringProvider: StringProvider,
        preferencesProvider: PreferencesProvider,
        cacheFileManager: CacheFileManager?
    ) {
        this.platformType = platformType
        mosaikRuntime.appDatabase = appDb
        mosaikRuntime.guidManager.appDatabase = appDb
        mosaikRuntime.cacheFileManager = cacheFileManager
        mosaikRuntime.stringProvider = stringProvider
        mosaikRuntime.preferencesProvider = preferencesProvider
        if (!initialized) {
            initialized = true
            mosaikRuntime.loadUrlEnteredByUser(appUrl)
        }
    }

    fun retryLoading(appUrl: String) {
        noAppLiveData.postValue(null)
        mosaikRuntime.loadUrlEnteredByUser(appUrl)
    }

    /**
     * return true when we are done
     */
    fun onWalletChosen(wallet: Wallet?): Boolean {
        return if (valueIdForWalletChooser != null && wallet != null) {
            // wallet should be chosen
            val valueId = valueIdForWalletChooser
            valueIdForWalletChooser = null
            valueId?.let {
                mosaikRuntime.setValue(
                    valueId,
                    wallet.getSortedDerivedAddressesList().map { it.publicAddress })
            }
            true
        } else {
            // address should be choosen
            walletForAddressChooser = wallet
            false
        }
    }

    fun onAddressChosen(addressDerivationIdx: Int) {
        val valueId = valueIdForAddressChooser ?: return
        val wallet = walletForAddressChooser ?: return

        valueIdForAddressChooser = null
        walletForAddressChooser = null

        mosaikRuntime.setValue(valueId, wallet.getDerivedAddress(addressDerivationIdx))
    }

    fun ergoPayActionCompleted() {
        ergoPayOnFinishedActionId?.let { actionId ->
            ergoPayOnFinishedActionId = null
            mosaikRuntime.runAction(actionId)
        }
    }

    fun ergoAuthActionCompleted() {
        ergoAuthOnFinishedActionId?.let { actionId ->
            ergoAuthOnFinishedActionId = null
            mosaikRuntime.runAction(actionId)
        }
    }

    fun qrCodeScanned(qrCode: String) {
        scanQrCodeActionId?.let { actionId ->
            scanQrCodeActionId = null
            mosaikRuntime.qrCodeScanned(actionId, qrCode)
        }
    }
}