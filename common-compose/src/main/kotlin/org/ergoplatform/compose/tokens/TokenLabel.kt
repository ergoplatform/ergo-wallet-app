package org.ergoplatform.compose.tokens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.minIconSize
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

@Composable
fun TokenLabel(
    tokenData: TokenEntryViewData,
    modifier: Modifier = Modifier,
    labelStyle: LabelStyle = LabelStyle.HEADLINE2,
) {
    Row(modifier) {
        // TODO token thumbnail

        Text(
            tokenData.displayedName ?: "",
            Modifier.weight(1f, false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = labelStyle(labelStyle),
        )

        tokenData.genuityFlag?.let { getGenuineImageVector(it) }?.let {
            Icon(
                it,
                null,
                Modifier.padding(start = defaultPadding / 2).size(minIconSize)
                    .align(Alignment.CenterVertically),
                tint = MosaikStyleConfig.primaryLabelColor
            )
        }
    }
}