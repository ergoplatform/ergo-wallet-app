package org.ergoplatform.android.ui

import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import org.ergoplatform.android.R
import org.ergoplatform.mosaik.MosaikStyleConfig

@Composable
fun AppComposeTheme(content: @Composable () -> Unit) {
    prepareMosaikConfig()

    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = colorResource(id = R.color.primary),
            surface = colorResource(id = R.color.cardview_background),
            onSurface = colorResource(id = R.color.text_color),
            isLight = LocalContext.current.resources.getBoolean(R.bool.isLight)
        )
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorResource(id = R.color.text_color),
            LocalContentAlpha provides 1f,
            content = content
        )
    }
}

@Composable
private fun prepareMosaikConfig() {
    MosaikStyleConfig.apply {
        primaryLabelColor = colorResource(id = R.color.primary)
        secondaryLabelColor = colorResource(id = R.color.darkgrey)
        defaultLabelColor = colorResource(id = R.color.text_color)
        primaryButtonTextColor = colorResource(id = R.color.textcolor)
        secondaryButtonTextColor = colorResource(id = R.color.textcolor)
        secondaryButtonColor = colorResource(id = R.color.secondary)
        textButtonTextColor = colorResource(id = R.color.primary)
        textButtonColorDisabled = secondaryLabelColor
    }
}