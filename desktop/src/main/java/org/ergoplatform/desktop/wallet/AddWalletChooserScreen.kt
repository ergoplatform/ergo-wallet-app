package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun AddWalletChooserScreen(
    gotoScreen: (ScreenConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()

        Column(
            Modifier.verticalScroll(scrollState).padding(defaultPadding)
                .widthIn(max = defaultMaxWidth).align(Alignment.Center)
        ) {
            AddWalletChooser(gotoScreen)

            Button(
                onClick = onDismiss,
                modifier = Modifier.padding(top = defaultPadding).align(Alignment.End),
                colors = primaryButtonColors()
            ) {
                Text("Back") // TODO i18n
            }
        }

        AppScrollbar(scrollState)
    }
}

@Composable
fun AddWalletChooser(gotoScreen: (ScreenConfig) -> Unit) {
    Column {
        ChooserEntry(
            { },
            Application.texts.getString(STRING_LABEL_CREATE_WALLET),
            Application.texts.getString(STRING_DESC_CREATE_WALLET),
            Icons.Default.Eco
        )

        ChooserEntry(
            { gotoScreen(ScreenConfig.RestoreWallet) },
            Application.texts.getString(STRING_LABEL_RESTORE_WALLET),
            Application.texts.getString(STRING_DESC_RESTORE_WALLET),
            Icons.Default.SettingsBackupRestore
        )

        ChooserEntry(
            { gotoScreen(ScreenConfig.AddReadOnlyWallet) },
            Application.texts.getString(STRING_LABEL_READONLY_WALLET),
            Application.texts.getString(STRING_DESC_READONLY_WALLET),
            Icons.Default.ZoomIn
        )

    }
}

@Composable
private fun ChooserEntry(onClick: () -> Unit, title: String, desc: String, icon: ImageVector) {
    AppCard(Modifier.fillMaxWidth().padding(bottom = defaultPadding)) {

        Row(Modifier.clickable { onClick() }.padding(defaultPadding)) {
            Icon(
                icon,
                null,
                Modifier.size(bigIconSize).align(Alignment.CenterVertically),
            )

            Column(Modifier.padding(start = defaultPadding)) {
                Text(
                    title,
                    Modifier.padding(bottom = defaultPadding),
                    style = labelStyle(LabelStyle.HEADLINE2),
                    color = uiErgoColor,
                    maxLines = 1,
                )
                Text(desc, style = labelStyle(LabelStyle.BODY1))
            }
        }

    }
}