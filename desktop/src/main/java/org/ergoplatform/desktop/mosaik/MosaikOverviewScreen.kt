package org.ergoplatform.desktop.mosaik

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.appTextFieldColors
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.MosaikAppEntry
import org.ergoplatform.mosaik.MosaikAppSuggestion
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils

@Composable
fun MosaikOverviewScreen(
    favoritesList: List<MosaikAppEntry>,
    lastVisitedList: List<MosaikAppEntry>,
    suggestionList: List<MosaikAppSuggestion>,
    onAddressEntered: (String) -> Unit,
    onAppClicked: (MosaikAppEntry) -> Unit,
) {
    AppScrollingLayout {
        Column(
            Modifier.padding(defaultPadding).widthIn(max = defaultMaxWidth)
                .align(Alignment.TopCenter)
        ) {
            val addressTextFieldValue =
                remember(lastVisitedList) { mutableStateOf(TextFieldValue()) }
            val onClickOrEnter: () -> Unit = { onAddressEntered(addressTextFieldValue.value.text) }

            if (favoritesList.isEmpty())
                Text(
                    remember { Application.texts.getString(STRING_DESC_MOSAIK) },
                    Modifier.padding(defaultPadding * 1.5f),
                    textAlign = TextAlign.Center,
                )

            OutlinedTextField(
                addressTextFieldValue.value,
                onValueChange = {
                    addressTextFieldValue.value = it
                },
                Modifier.fillMaxWidth().addOnEnterListener(onClickOrEnter),
                maxLines = 1,
                singleLine = true,
                label = { Text(Application.texts.getString(STRING_HINT_APP_URL)) },
                trailingIcon = {
                    IconButton(onClick = onClickOrEnter) {
                        Icon(Icons.Default.ArrowForward, null)
                    }
                },
                colors = appTextFieldColors(),
            )

            Text(
                remember { Application.texts.getString(STRING_TITLE_FAVORITES) },
                Modifier.padding(top = defaultPadding * 1.5f),
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = uiErgoColor,
            )
            MosaikAppList(favoritesList, onAppClicked)

            Text(
                remember { Application.texts.getString(STRING_TITLE_LAST_VISITED) },
                Modifier.padding(top = defaultPadding * 1.5f),
                style = labelStyle(LabelStyle.BODY1BOLD),
                color = uiErgoColor,
            )
            MosaikAppList(lastVisitedList, onAppClicked)

            if (suggestionList.isNotEmpty()) {
                Text(
                    remember { Application.texts.getString(STRING_TITLE_SUGGESTIONS) },
                    Modifier.padding(top = defaultPadding * 1.5f),
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor,
                )
                MosaikAppList(suggestionList.map {
                    MosaikAppEntry(
                        it.appUrl,
                        it.appName,
                        it.appDescription,
                        null,
                        0,
                        false,
                        notificationUrl = null,
                    )
                }, onAppClicked)
            }
        }
    }
}

@Composable
private fun MosaikAppList(
    appList: List<MosaikAppEntry>,
    onAppClicked: (MosaikAppEntry) -> Unit,
) {

    Column(Modifier.padding(horizontal = defaultPadding / 2)) {
        if (appList.isEmpty()) {
            Text(
                remember { Application.texts.getString(STRING_DESC_NONE) },
                style = labelStyle(LabelStyle.BODY1),
            )
        }

        appList.forEach { mosaikApp ->
            AppCard(
                Modifier.padding(top = defaultPadding / 2, bottom = defaultPadding / 4)
                    .clickable { onAppClicked(mosaikApp) }) {

                Row(Modifier.padding(defaultPadding / 2)) {
                    val appIconModifier =
                        Modifier.align(Alignment.CenterVertically).size(bigIconSize)

                    AppIcon(mosaikApp, appIconModifier)

                    Column(Modifier.weight(1f).padding(horizontal = defaultPadding / 2)) {
                        Text(
                            mosaikApp.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = labelStyle(LabelStyle.BODY1BOLD),
                            color = uiErgoColor,
                        )

                        if (mosaikApp.favorite && mosaikApp.notificationUnread && mosaikApp.lastNotificationMessage != null) {
                            Card(modifier = Modifier.border(1.dp, uiErgoColor)) {
                                Text(
                                    mosaikApp.lastNotificationMessage!!,
                                    Modifier.padding(4.dp),
                                    style = labelStyle(LabelStyle.BODY1),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            Text(
                                if (mosaikApp.description.isNullOrBlank()) mosaikApp.url else mosaikApp.description!!,
                                style = labelStyle(LabelStyle.BODY1),
                                maxLines = if (mosaikApp.description.isNullOrBlank()) 1 else 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

            }
        }
    }

}

@Composable
private fun AppIcon(
    mosaikApp: MosaikAppEntry,
    modifier: Modifier
) {
    // set icon, if we have one
    val imageBitmap = mosaikApp.iconFile?.let { iconFile ->
        remember(iconFile) {
            Application.filesCache.readFileContent(iconFile)?.let { imageBytes ->
                try {
                    loadImageBitmap(imageBytes.inputStream())
                } catch (t: Throwable) {
                    LogUtils.logDebug(
                        "AppOverView",
                        "Could not read icon file",
                        t
                    )
                    null
                }
            }
        }
    }

    if (imageBitmap == null)
        Icon(
            Icons.Default.AutoAwesomeMosaic,
            mosaikApp.name,
            modifier,
            tint = MosaikStyleConfig.defaultLabelColor
        )
    else
        Image(
            imageBitmap,
            mosaikApp.name,
            modifier
        )
}