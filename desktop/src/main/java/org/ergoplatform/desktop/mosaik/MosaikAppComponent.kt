package org.ergoplatform.desktop.mosaik

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.loadImageBitmap
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.Application
import org.ergoplatform.desktop.appVersionString
import org.ergoplatform.desktop.ui.copyToClipoard
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.MosaikContext
import org.ergoplatform.mosaik.model.MosaikManifest
import org.ergoplatform.mosaik.model.actions.ErgoAuthAction
import org.ergoplatform.mosaik.model.actions.ErgoPayAction
import org.ergoplatform.mosaik.model.actions.TokenInformationAction

class MosaikAppComponent(
    private val appTitle: String?,
    private val appUrl: String,
    private val componentContext: ComponentContext,
    navHost: NavHostComponent,
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = manifestState.value?.appName ?: appTitle ?: appUrl

    private val isFavoriteState = mutableStateOf(false)

    override val actions: @Composable RowScope.() -> Unit
        get() = {
            IconButton(
                mosaikRuntime::switchFavorite,
                enabled = mosaikRuntime.appUrl != null,
            ) {
                Icon(
                    if (isFavoriteState.value) Icons.Default.Star
                    else Icons.Default.StarBorder,
                    null
                )
            }
        }

    private val mosaikRuntime = object : AppMosaikRuntime(
        "Ergo Wallet App (Desktop)",
        appVersionString,
        { MosaikContext.Platform.DESKTOP },
        MosaikGuidManager().apply {
            appDatabase = Application.database
        },
    ) {
        override val coroutineScope: CoroutineScope
            get() = componentScope()

        override fun openBrowser(url: String) {
            org.ergoplatform.desktop.ui.openBrowser(url)
        }

        override fun pasteToClipboard(text: String) {
            text.copyToClipoard()
        }

        override fun runErgoAuthAction(action: ErgoAuthAction) {
            // TODO ErgoAuth
            navHost.showErrorDialog("ErgoAuth not available yet")
        }

        override fun runErgoPayAction(action: ErgoPayAction) {
            TODO("Not yet implemented")
        }

        override fun runTokenInformationAction(action: TokenInformationAction) {
            // TODO tokenInfo
            navHost.showErrorDialog("TokenInfo not available yet")
        }

        override fun scanQrCode(actionId: String) {
            TODO("Not yet implemented")
        }

        override fun showDialog(dialog: MosaikDialog) {
            navHost.dialogHandler.showDialog(dialog)
        }

        override fun showErgoAddressChooser(valueId: String) {
            TODO("Not yet implemented")
        }

        override fun showErgoWalletChooser(valueId: String) {
            TODO("Not yet implemented")
        }

        override fun onAppNavigated(manifest: MosaikManifest) {
            manifestState.value = manifest
            isFavoriteState.value = isFavoriteApp
        }

        override fun noAppLoaded(cause: Throwable) {
            noAppLoadedErrorState.value = cause
        }
    }

    init {
        lifecycle.doOnResume {
            mosaikRuntime.checkViewTreeValidity()
        }

        mosaikRuntime.appDatabase = Application.database
        mosaikRuntime.guidManager.appDatabase = Application.database
        mosaikRuntime.cacheFileManager = Application.filesCache
        mosaikRuntime.stringProvider = Application.texts
        mosaikRuntime.preferencesProvider = Application.prefs

        MosaikLogger.logger = MosaikLogger.DefaultLogger
        MosaikComposeConfig.convertByteArrayToImageBitmap =
            { imageBytes -> loadImageBitmap(imageBytes.inputStream()) }
        MosaikComposeConfig.interceptReturnForImeAction = true
        MosaikComposeConfig.DropDownMenu = { expanded,
                                             dismiss,
                                             modifier,
                                             content ->
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = dismiss,
                modifier = modifier,
                content = content,
            )
        }

        MosaikComposeConfig.DropDownItem = { onClick, content ->
            DropdownMenuItem(onClick = onClick, content = content)
        }

        mosaikRuntime.loadUrlEnteredByUser(appUrl)
    }

    private val noAppLoadedErrorState = mutableStateOf<Throwable?>(null)
    private val manifestState = mutableStateOf<MosaikManifest?>(null)

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        MosaikAppScreen(
            mosaikRuntime.viewTree,
            noAppLoadedErrorState,
            retryClicked = {
                noAppLoadedErrorState.value = null
                mosaikRuntime.loadMosaikApp(appUrl)
            }
        )
    }

    override fun onNavigateBack(): Boolean = mosaikRuntime.navigateBack()
}