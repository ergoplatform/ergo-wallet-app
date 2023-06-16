package org.ergoplatform.desktop.tokens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NFT_AUDIO
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NFT_IMG
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NFT_VID
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NONE
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_AUDIO
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_GENERIC
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_IMAGE
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_VIDEO
import org.ergoplatform.uilogic.tokens.FilterTokenListUiLogic

@Composable
fun TokenFilterMenu(uiLogic: FilterTokenListUiLogic) {
    var showActionMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showActionMenu = !showActionMenu }) {
        Icon(Icons.Default.FilterAlt, null)
        DropdownMenu(
            showActionMenu,
            onDismissRequest = { showActionMenu = false },
            modifier = Modifier.widthIn(min = 150.dp)
        ) {
            val modifierForFilter: (Int) -> Modifier = {
                Modifier.alpha(if (uiLogic.hasTokenFilter(it)) 1f else 0f)
            }

            DropdownMenuItem(onClick = {
                uiLogic.toggleTokenFilter(THUMBNAIL_TYPE_NONE)
                showActionMenu = false
            }) {
                Icon(Icons.Default.Check, null, modifierForFilter(THUMBNAIL_TYPE_NONE))
                Spacer(Modifier.size(defaultPadding))
                Text(Application.texts.getString(STRING_LABEL_TOKEN_GENERIC))
            }
            DropdownMenuItem(onClick = {
                uiLogic.toggleTokenFilter(THUMBNAIL_TYPE_NFT_IMG)
                showActionMenu = false
            }) {
                Icon(Icons.Default.Check, null, modifierForFilter(THUMBNAIL_TYPE_NFT_IMG))
                Spacer(Modifier.size(defaultPadding))
                Text(Application.texts.getString(STRING_LABEL_TOKEN_IMAGE))
            }
            DropdownMenuItem(onClick = {
                uiLogic.toggleTokenFilter(THUMBNAIL_TYPE_NFT_AUDIO)
                showActionMenu = false
            }) {
                Icon(Icons.Default.Check, null, modifierForFilter(THUMBNAIL_TYPE_NFT_AUDIO))
                Spacer(Modifier.size(defaultPadding))
                Text(Application.texts.getString(STRING_LABEL_TOKEN_AUDIO))
            }
            DropdownMenuItem(onClick = {
                uiLogic.toggleTokenFilter(THUMBNAIL_TYPE_NFT_VID)
                showActionMenu = false
            }) {
                Icon(Icons.Default.Check, null, modifierForFilter(THUMBNAIL_TYPE_NFT_VID))
                Spacer(Modifier.size(defaultPadding))
                Text(Application.texts.getString(STRING_LABEL_TOKEN_VIDEO))
            }
        }
    }
}