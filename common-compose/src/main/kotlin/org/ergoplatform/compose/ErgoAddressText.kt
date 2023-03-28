package org.ergoplatform.compose

import androidx.compose.foundation.clickable
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.uilogic.StringProvider

@Composable
fun ErgoAddressText(
    address: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    var showFull by remember { mutableStateOf(false) }
    val myModifier = modifier.clickable { showFull = !showFull }

    if (!showFull)
        MiddleEllipsisText(
            address,
            myModifier,
            style = style,
            color = color,
            textAlign = textAlign,
        )
    else
        Text(
            address, myModifier, style = style, color = color,
            textAlign = textAlign,
        )
}

@Composable
fun ErgoAddressText(
    address: String,
    getDb: () -> IAppDatabase,
    texts: StringProvider,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val addressLabelState = remember(address) { mutableStateOf<String?>(null) }
    LaunchedEffect(address) {
        withContext(Dispatchers.IO) {
            getAddressLabelFromDatabase(getDb(), address, texts)?.let {
                addressLabelState.value = it
            }
        }
    }

    if (addressLabelState.value != null)
        Text(
            addressLabelState.value!!,
            modifier,
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    else
        ErgoAddressText(
            address,
            modifier,
            style,
            color,
            textAlign,
        )
}