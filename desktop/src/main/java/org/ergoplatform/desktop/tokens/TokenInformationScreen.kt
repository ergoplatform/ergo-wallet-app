package org.ergoplatform.desktop.tokens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.compose.settings.minIconSize
import org.ergoplatform.compose.settings.smallIconSize
import org.ergoplatform.compose.tokens.TokenLabel
import org.ergoplatform.compose.tokens.TokenThumbnail
import org.ergoplatform.compose.tokens.getThumbnailDrawableId
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.getExplorerTokenUrl
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic

@Composable
fun TokenInformationScreen(
    tokenLayoutLogic: TokenInformationComponent.DesktopTokenInformationLayoutLogic?,
    infoLoading: Boolean,
    downloadState: TokenInformationModelLogic.StateDownload,
    startDownload: () -> Unit,
) {
    AppScrollingLayout {
        if (infoLoading) {
            AppProgressIndicator()

        } else if (tokenLayoutLogic == null || tokenLayoutLogic.tokenInformation == null) {
            Text(
                remember { Application.texts.getString(STRING_LABEL_ERROR_FETCHING) },
                Modifier.align(Alignment.Center).padding(horizontal = defaultPadding)
            )

        } else {

            TokenInfoLayout(
                tokenLayoutLogic,
                downloadState,
                startDownload,
            )

        }
    }
}

@Composable
private fun BoxScope.TokenInfoLayout(
    tokenLayoutLogic: TokenInformationComponent.DesktopTokenInformationLayoutLogic,
    downloadState: TokenInformationModelLogic.StateDownload,
    startDownload: () -> Unit,
) {
    Column(
        Modifier.widthIn(max = defaultMaxWidth).align(Alignment.Center).padding(defaultPadding)
    ) {

        TokenLabel(
            tokenLayoutLogic.tokenInformation!!, amountText = null,
            Modifier.align(Alignment.CenterHorizontally),
            showTumbnail = false
        )

        MiddleEllipsisText(
            tokenLayoutLogic.tokenId!!,
            Modifier.align(Alignment.CenterHorizontally).padding(vertical = defaultPadding / 2)
                .clickable { openBrowser(getExplorerTokenUrl(tokenLayoutLogic.tokenId!!)) },
        )

        if (tokenLayoutLogic.nftLayoutVisible) {
            NftLayout(
                tokenLayoutLogic,
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                downloadState,
                startDownload,
            )
        }

        tokenLayoutLogic.balanceAmount?.let { balanceAmount ->
            Text(
                remember { Application.texts.getString(STRING_TITLE_WALLET_BALANCE) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )

            Text(balanceAmount, Modifier.align(Alignment.CenterHorizontally))

            tokenLayoutLogic.balanceValue?.let {
                Text(
                    it,
                    Modifier.align(Alignment.CenterHorizontally),
                    color = foregroundColor(ForegroundColor.SECONDARY)
                )
            }
        }

        tokenLayoutLogic.supplyAmount?.let {
            Text(
                remember { Application.texts.getString(STRING_LABEL_TOKEN_SUPPLY) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )
            Text(it, Modifier.align(Alignment.CenterHorizontally))
        }

        tokenLayoutLogic.description?.let {
            Text(
                remember { Application.texts.getString(STRING_LABEL_TOKEN_DESCRIPTION) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )
            Text(it, Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        }

        Text(
            remember { Application.texts.getString(STRING_LABEL_MINTING_TX) },
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding).clickable {
                openBrowser(getExplorerTxUrl(tokenLayoutLogic.tokenInformation!!.mintingTxId))
            },
            style = labelStyle(LabelStyle.BODY1LINK),
        )
    }
}

@Composable
private fun NftLayout(
    tokenLayoutLogic: TokenInformationComponent.DesktopTokenInformationLayoutLogic,
    modifier: Modifier,
    downloadState: TokenInformationModelLogic.StateDownload,
    startDownload: () -> Unit,
) {
    Column(modifier) {
        val nftPreviewPicBoxModifier = Modifier.padding(horizontal = defaultPadding)
            .heightIn(100.dp, 600.dp).fillMaxWidth()
        when (downloadState) {
            TokenInformationModelLogic.StateDownload.NOT_AVAILABLE -> {}
            TokenInformationModelLogic.StateDownload.NOT_STARTED -> {
                Card(modifier = Modifier.border(1.dp, uiErgoColor)) {
                    Column(Modifier.padding(defaultPadding)) {
                        Text(
                            remember { Application.texts.getString(STRING_DESC_DOWNLOAD_CONTENT) },
                            Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        AppButton(
                            startDownload,
                            Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(remember {
                                Application.texts.getString(STRING_BUTTON_DOWNLOAD_CONTENT_ON)
                            })
                        }
                    }
                }
            }

            TokenInformationModelLogic.StateDownload.RUNNING ->
                Box(nftPreviewPicBoxModifier, contentAlignment = Alignment.Center) {
                    AppProgressIndicator()
                }

            TokenInformationModelLogic.StateDownload.DONE ->
                Box(nftPreviewPicBoxModifier, contentAlignment = Alignment.Center) {
                    NftImagePreview(tokenLayoutLogic, Modifier)
                }

            TokenInformationModelLogic.StateDownload.ERROR ->
                Box(nftPreviewPicBoxModifier, contentAlignment = Alignment.Center) {
                    ImageError(Modifier)
                }

        }

        getThumbnailDrawableId(tokenLayoutLogic.nftThumnailType)?.let { thumbnail ->
            TokenThumbnail(
                thumbnail,
                smallIconSize * 4,
                Modifier.padding(end = org.ergoplatform.compose.settings.defaultPadding / 2)
                    .align(Alignment.CenterHorizontally)
            )
        }

        tokenLayoutLogic.nftContentLink?.let { contentLink ->
            val isLink = remember(contentLink) { contentLink.contains("://") }
            Text(
                remember { Application.texts.getString(STRING_LABEL_CONTENT_LINK) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )
            Text(
                contentLink,
                (if (isLink) Modifier.clickable {
                    openBrowser(contentLink)
                } else Modifier).align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                style = labelStyle(if (isLink) LabelStyle.BODY1LINK else LabelStyle.BODY1),
                maxLines = 5,
            )
        }

        tokenLayoutLogic.nftContentHash?.let {
            Text(
                remember { Application.texts.getString(STRING_LABEL_CONTENT_HASH) },
                Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
                style = labelStyle(LabelStyle.BODY1BOLD),
            )
            Row(Modifier.align(Alignment.CenterHorizontally).clickable { it.copyToClipboard() }) {
                Text(
                    it,
                    Modifier.weight(1f, false),
                    textAlign = TextAlign.Center,
                )
                tokenLayoutLogic.nftHashValidated?.let { hashValid ->
                    Icon(
                        when (hashValid) {
                            true -> Icons.Default.Verified
                            false -> Icons.Default.Report
                        },
                        null,
                        Modifier.padding(start = org.ergoplatform.compose.settings.defaultPadding / 2)
                            .size(
                                minIconSize
                            )
                            .align(Alignment.CenterVertically),
                        tint = MosaikStyleConfig.primaryLabelColor
                    )
                }
            }
        }
    }
}

@Composable
private fun NftImagePreview(
    tokenLayoutLogic: TokenInformationComponent.DesktopTokenInformationLayoutLogic,
    modifier: Modifier
) {
    val imageBytes = tokenLayoutLogic.nftDownloadContent
    val imageBitmap = remember(imageBytes) {
        if (imageBytes != null && imageBytes.isNotEmpty()) {

            try {
                loadImageBitmap(imageBytes.inputStream())
            } catch (t: Throwable) {
                MosaikLogger.logError("Could not load bitmap", t)
                null
            }
        } else null
    }

    if (imageBitmap != null) {
        Image(imageBitmap, null, modifier, contentScale = ContentScale.Inside)
    } else {
        ImageError(modifier)
    }
}

@Composable
private fun ImageError(modifier: Modifier) {
    Icon(
        Icons.Default.Warning,
        null,
        modifier.size(smallIconSize * 4),
    )
}
