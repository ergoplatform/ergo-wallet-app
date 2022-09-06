package org.ergoplatform.desktop.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.ergoplatform.Application
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.desktop.ui.AppDialog
import org.ergoplatform.desktop.ui.AppScrollbar
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_LABEL_CG_CONN_ERROR
import org.ergoplatform.uilogic.STRING_LABEL_NONE

@Composable
fun DisplayCurrencyListDialog(onDismissRequest: () -> Unit, onCurrencyChosen: (String) -> Unit) {
    AppDialog(onDismissRequest) {
        Box(Modifier.fillMaxWidth().animateContentSize()) {
            val currencyListState =
                WalletStateSyncManager.getInstance().currencies.collectAsState()
            val currencyList = currencyListState.value

            if (currencyList != null && currencyList.isNotEmpty()) {
                val listWithNone = arrayListOf<String>()
                listWithNone.add("")
                listWithNone.addAll(currencyList.sorted())

                val scrollState = rememberScrollState()
                Column(
                    Modifier.fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    listWithNone.forEach { currency ->
                        Text(
                            if (currency.isEmpty()) Application.texts.getString(
                                STRING_LABEL_NONE
                            ) else currency.uppercase(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCurrencyChosen(currency)
                                    onDismissRequest()
                                }.padding(
                                    horizontal = defaultPadding,
                                    vertical = defaultPadding / 2
                                ),
                            style = labelStyle(LabelStyle.HEADLINE2),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                AppScrollbar(scrollState)
            } else if (currencyList != null && currencyList.isEmpty()) {
                Text(
                    Application.texts.getString(STRING_LABEL_CG_CONN_ERROR),
                    Modifier.align(Alignment.Center).padding(defaultPadding),
                    style = labelStyle(LabelStyle.HEADLINE2),
                    textAlign = TextAlign.Center,
                )
            } else {
                AppProgressIndicator()
            }
        }
    }
}
