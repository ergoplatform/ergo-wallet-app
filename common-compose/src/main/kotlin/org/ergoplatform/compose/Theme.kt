package org.ergoplatform.compose.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ergoplatform.mosaik.MosaikStyleConfig

val defaultPadding = 16.dp
val defaultMaxWidth = 600.dp
val bigIconSize = 58.dp
val smallIconSize = 24.dp
val minIconSize = 18.dp

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
    errorCursorColor = MosaikStyleConfig.primaryLabelColor,
    errorLeadingIconColor = MosaikStyleConfig.primaryLabelColor,
)

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = primaryButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick,
        modifier,
        enabled,
        shape = RoundedCornerShape(MosaikStyleConfig.buttonShapeRadius),
        contentPadding = MosaikStyleConfig.buttonPadding,
        colors = colors,
    ) {
        content()
    }
}

@Composable
fun AppCard(modifier: Modifier, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(MosaikStyleConfig.cardShapeRadius),
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        content()
    }
}
