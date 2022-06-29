package org.ergoplatform.desktop.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.appVersionString
import org.ergoplatform.desktop.ui.LinkifyText
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.secondaryButtonColors
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.drawVerticalScrollbar
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.*

@Composable
fun SettingsScreen(
    currencyButtonTextState: MutableState<String>,
    onChangeCurrencyClicked: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column {
        Column(Modifier.fillMaxWidth()) {
            Image(
                painterResource("symbol_bold__1080px__black.svg"),
                null,
                colorFilter = ColorFilter.tint(MosaikStyleConfig.secondaryButtonTextColor),
                modifier = Modifier.height(90.dp).align(Alignment.CenterHorizontally)
                    .padding(defaultPadding)
            )
            Text(
                Application.texts.getString(STRING_APP_NAME),
                Modifier.align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.HEADLINE1)
            )
            Text(
                appVersionString,
                Modifier.align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.BODY2)
            )
            Text(
                Application.texts.getString(
                    STRING_DESC_ABOUT, Application.texts.getString(
                        STRING_ABOUT_YEAR
                    )
                ),
                Modifier.padding(defaultPadding / 2).align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.BODY1)
            )
            LinkifyText(
                Application.texts.getString(STRING_DESC_ABOUT_MOREINFO),
                Modifier.padding(
                    bottom = defaultPadding,
                    start = defaultPadding / 2,
                    end = defaultPadding / 2
                ).align(Alignment.CenterHorizontally),
                style = labelStyle(LabelStyle.BODY1),
                isHtml = true
            )
        }

        Column(
            Modifier.fillMaxSize().drawVerticalScrollbar(scrollState).verticalScroll(scrollState)
        ) {

            Card(Modifier.widthIn(max = 600.dp).align(Alignment.CenterHorizontally)) {
                Column(Modifier.padding(defaultPadding)) {
                    LinkifyText(
                        Application.texts.getString(STRING_DESC_COINGECKO),
                        Modifier.padding(defaultPadding / 2).align(Alignment.CenterHorizontally),
                        style = labelStyle(LabelStyle.BODY1),
                        isHtml = true
                    )

                    Button(
                        onClick = { onChangeCurrencyClicked() },
                        colors = secondaryButtonColors(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(currencyButtonTextState.value)
                    }
                }

            }

        }
    }
}