package org.ergoplatform.compose.tokens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.TokenAmount
import org.ergoplatform.compose.ComposePlatformUtils
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.minIconSize
import org.ergoplatform.compose.settings.smallIconSize
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.foregroundColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.mosaik.model.ui.text.TokenLabel
import org.ergoplatform.persistance.TokenDbProvider
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.ergoplatform.uilogic.StringProvider

/**
 * TokenLabel shows a decorated token label with logo/preview pic and genuine information.
 * For simpler amount token labels, use TokenEntryView
 */
@Composable
fun TokenLabel(
    tokenData: TokenEntryViewData,
    modifier: Modifier = Modifier,
    labelStyle: LabelStyle = LabelStyle.HEADLINE2,
    textColor: ForegroundColor = ForegroundColor.DEFAULT,
    showAmount: Boolean = true,
    showTumbnail: Boolean = true,
    showGenuity: Boolean = true,
) {
    TokenLabel(
        tokenData.displayedName ?: "",
        if (showTumbnail) tokenData.thumbnailType else null,
        if (showAmount) tokenData.balance else null,
        if (showGenuity) tokenData.genuityFlag else null,
        modifier,
        labelStyle,
        textColor,
    )
}

@Composable
fun TokenLabel(
    tokenInformation: TokenInformation,
    amountText: String?,
    modifier: Modifier = Modifier,
    labelStyle: LabelStyle = LabelStyle.HEADLINE2,
    textColor: ForegroundColor = ForegroundColor.DEFAULT,
    showTumbnail: Boolean = true,
    showGenuity: Boolean = true,
) {
    TokenLabel(
        tokenInformation.displayName,
        if (showTumbnail) tokenInformation.thumbnailType else null,
        amountText,
        if (showGenuity) tokenInformation.genuineFlag else null,
        modifier,
        labelStyle,
        textColor,
    )
}

fun getAppMosaikTokenLabelBuilder(
    tokenDb: () -> TokenDbProvider,
    apiService: () -> ApiServiceManager,
    stringResolver: () -> StringProvider
): @Composable (
    properties: TokenLabel,
    modifier: Modifier,
    content: @Composable (tokenName: String, decimals: Int, modifier: Modifier) -> Unit
) -> Unit {

    return { properties, modifier, content ->
        val tokenInfoState = TokenInfoManager.getInstance()
            .getTokenInformationFlow(properties.tokenId, tokenDb(), apiService())
            .collectAsState(null, Dispatchers.IO)

        val tokenInfo = tokenInfoState.value

        val amountText =
            properties.amount?.let {
                TokenAmount(
                    it,
                    tokenInfo?.decimals ?: properties.decimals
                ).toString()
            }

        if (tokenInfo == null) {
            // no token info loaded yet, we fall back to default implementation
            content(
                properties.tokenName ?: stringResolver().getString(STRING_LABEL_UNNAMED_TOKEN),
                properties.decimals,
                modifier
            )
        } else {
            TokenLabel(
                tokenInfo,
                amountText,
                modifier,
                properties.style,
                properties.textColor,
                properties.isDecorated,
                properties.isDecorated
            )
        }
    }

}

@Composable
private fun TokenLabel(
    tokenNameText: String,
    thumbnailType: Int?,
    amountText: String?,
    genuityType: Int?,
    modifier: Modifier = Modifier,
    labelStyle: LabelStyle = LabelStyle.HEADLINE2,
    textColor: ForegroundColor = ForegroundColor.DEFAULT,
) {
    Row(modifier) {
        thumbnailType?.let { getThumbnailDrawableId(it) }?.let { thumbnail ->
            TokenThumbnail(
                thumbnail,
                smallIconSize,
                Modifier.padding(end = defaultPadding / 2).align(Alignment.CenterVertically)
            )
        }

        Text(
            (amountText?.let { "$it " } ?: "") +
                    tokenNameText,
            Modifier.weight(1f, false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = labelStyle(labelStyle),
            color = foregroundColor(textColor)
        )

        genuityType?.let { getGenuineImageVector(it) }?.let {
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
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(size)) {
        Icon(
            ComposePlatformUtils.getDrawablePainter(ComposePlatformUtils.Drawable.Octagon),
            null,
            tint = MosaikStyleConfig.secondaryLabelColor
        )

        Icon(
            ComposePlatformUtils.getDrawablePainter(thumbnail),
            null,
            Modifier.size(size * 0.56f).align(Alignment.Center),
            tint = MaterialTheme.colors.surface
        )
    }
}