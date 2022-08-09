package org.ergoplatform.compose.settings

import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.ergoplatform.mosaik.MosaikStyleConfig

val defaultPadding = 16.dp
val defaultMaxWidth = 600.dp
val bigIconSize = 58.dp

@Composable
fun primaryButtonColors() = buttonColors(
    MosaikStyleConfig.primaryLabelColor,
    MosaikStyleConfig.primaryButtonTextColor
)

@Composable
fun secondaryButtonColors() = buttonColors(
    MosaikStyleConfig.secondaryButtonColor,
    MosaikStyleConfig.secondaryButtonTextColor
)

@Composable
fun appTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = MaterialTheme.colors.onSurface,
    focusedLabelColor = MaterialTheme.colors.onSurface,
    errorLabelColor = MosaikStyleConfig.primaryLabelColor,
    errorBorderColor = MosaikStyleConfig.primaryLabelColor,
    errorTrailingIconColor = MosaikStyleConfig.primaryLabelColor,
)