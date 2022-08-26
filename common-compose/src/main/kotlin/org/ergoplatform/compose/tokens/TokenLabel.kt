package org.ergoplatform.compose.tokens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ergoplatform.compose.ComposePlatformUtils
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.minIconSize
import org.ergoplatform.compose.settings.smallIconSize
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

@Composable
fun TokenLabel(
    tokenData: TokenEntryViewData,
    modifier: Modifier = Modifier,
    labelStyle: LabelStyle = LabelStyle.HEADLINE2,
    showAmount: Boolean = true,
    showTumbnail: Boolean = true,
    showGenuity: Boolean = true,
) {
    Row(modifier) {
        if (showTumbnail)
            tokenData.thumbnailType?.let { getThumbnailDrawableId(it) }?.let { thumbnail ->
                TokenThumbnail(
                    thumbnail,
                    Modifier.padding(end = defaultPadding / 2).size(smallIconSize)
                        .align(Alignment.CenterVertically)
                )
            }

        Text(
            ((if (showAmount) tokenData.balance else null)?.let { "$it " } ?: "") +
                    (tokenData.displayedName ?: ""),
            Modifier.weight(1f, false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = labelStyle(labelStyle),
        )

        if (showGenuity)
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

@Composable
fun TokenThumbnail(
    thumbnail: ComposePlatformUtils.Drawable,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Icon(
            ComposePlatformUtils.getDrawablePainter(ComposePlatformUtils.Drawable.Octagon),
            null,
            tint = MosaikStyleConfig.secondaryLabelColor
        )

        Icon(
            ComposePlatformUtils.getDrawablePainter(thumbnail),
            null,
            Modifier.size(smallIconSize * 0.56f).align(Alignment.Center),
            tint = MaterialTheme.colors.surface
        )
    }
}