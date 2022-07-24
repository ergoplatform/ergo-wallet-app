package org.ergoplatform.desktop.transactions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.*
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.transactions.coldSigningRequestToQrChunks
import org.ergoplatform.uilogic.*

@Composable
fun SigningPromptDialog(
    signingPrompt: String,
    onContinueClicked: () -> Unit,
    pagesScanned: Int?,
    pagesToScan: Int?,
    onDismissRequest: () -> Unit,
) {
    var lowRes by remember { mutableStateOf(false) }

    AppDialog(onDismissRequest, verticalPadding = defaultPadding * 6) {
        Box {
            val scrollState = rememberScrollState()
            Box(Modifier.fillMaxHeight().verticalScroll(scrollState)) {
                Column(
                    Modifier.padding(defaultPadding).padding(top = defaultPadding)
                        .align(Alignment.Center)
                ) {

                    PagedQrContainer(
                        lowRes,
                        calcChunks = { limit ->
                            coldSigningRequestToQrChunks(
                                signingPrompt, limit
                            )
                        },
                        onContinueClicked,
                        lastPageButtonLabel = STRING_BUTTON_SCAN_SIGNED_TX,
                        descriptionLabel = STRING_DESC_PROMPT_SIGNING_MULTIPLE,
                        lastPageDescriptionLabel = STRING_DESC_PROMPT_SIGNING,
                    )

                    // shown when first pages are scanned
                    if ((pagesScanned ?: 0) > 0) {
                        Text(
                            remember(pagesScanned) {
                                Application.texts.getString(
                                    STRING_LABEL_QR_PAGES_INFO,
                                    pagesScanned!!,
                                    pagesToScan!!
                                )
                            },
                            Modifier.align(Alignment.CenterHorizontally)
                                .padding(horizontal = defaultPadding / 2),
                            style = labelStyle(LabelStyle.HEADLINE2),
                        )
                        // scrolls to bottom on every new page scanned
                        LaunchedEffect(pagesScanned) {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    }
                }

                IconButton(onDismissRequest, Modifier.align(Alignment.TopStart)) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                // TODO cold wallet save/load functionality
                IconButton({ lowRes = !lowRes }, Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.BurstMode, null)
                }
            }
            AppScrollbar(scrollState)
        }
    }
}

@Composable
fun PagedQrContainer(
    lowRes: Boolean,
    calcChunks: (limit: Int) -> List<String>,
    onLastPageButtonClicked: () -> Unit,
    lastPageButtonLabel: String,
    descriptionLabel: String,
    lastPageDescriptionLabel: String,
) {
    var page by remember(lowRes) { mutableStateOf(0) }
    val qrPages = remember(lowRes) {
        calcChunks(if (lowRes) QR_DATA_LENGTH_LOW_RES else QR_DATA_LENGTH_LIMIT)
    }
    val qrImage = remember(page, qrPages) { getQrCodeImageBitmap(qrPages[page]) }
    val lastPage = page == qrPages.size - 1

    Column {
        Row(Modifier.fillMaxWidth()) {
            IconButton(
                { page-- },
                Modifier.align(Alignment.CenterVertically),
                enabled = page > 0
            ) { Icon(Icons.Default.ChevronLeft, null) }
            Image(
                qrImage,
                null,
                Modifier.height(400.dp).padding(vertical = defaultPadding)
                    .weight(1f)
            )
            IconButton(
                { page++ },
                Modifier.align(Alignment.CenterVertically),
                enabled = !lastPage
            ) { Icon(Icons.Default.ChevronRight, null) }
        }

        Text(
            remember(lastPage) {
                Application.texts.getString(
                    if (lastPage) lastPageDescriptionLabel
                    else descriptionLabel
                )
            },
            Modifier.fillMaxWidth().padding(horizontal = defaultPadding),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY1),
        )

        if (qrPages.size > 1)
            Text(
                remember(page) {
                    Application.texts.getString(STRING_LABEL_QR_PAGES_INFO, page + 1, qrPages.size)
                },
                Modifier.align(Alignment.CenterHorizontally).padding(defaultPadding / 2),
                style = labelStyle(LabelStyle.HEADLINE2),
            )

        Button(
            { if (!lastPage) page += 1 else onLastPageButtonClicked() },
            Modifier.align(Alignment.CenterHorizontally).padding(top = defaultPadding),
            colors = primaryButtonColors()
        ) {
            Text(remember(lastPage) { Application.texts.getString(if (!lastPage) STRING_BUTTON_NEXT else lastPageButtonLabel) })
        }
    }

}