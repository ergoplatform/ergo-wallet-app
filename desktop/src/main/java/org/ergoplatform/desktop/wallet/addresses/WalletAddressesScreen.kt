package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.AppCard
import org.ergoplatform.desktop.ui.defaultMaxWidth
import org.ergoplatform.desktop.ui.defaultPadding
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_BUTTON_ADD_ADDRESSES
import org.ergoplatform.uilogic.STRING_DESC_WALLET_ADDRESSES
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.addresses.isDerivedAddress

@Composable
fun WalletAddressesScreen(
    addressList: List<WalletAddress>,
    wallet: Wallet?,
    onDeriveAddresses: ((Int) -> Unit)?,
) {
    val state = rememberLazyListState()
    Box {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            items(addressList.size + 1) { index ->
                if (index < addressList.size)
                    WalletAddressCard(
                        addressList[index],
                        wallet
                    )
                else
                    AddAddressCard(onDeriveAddresses = onDeriveAddresses)
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}

@Composable
fun WalletAddressCard(
    walletAddress: WalletAddress,
    wallet: Wallet?,
) {
    AppCard(
        modifier = Modifier.padding(defaultPadding).defaultMinSize(400.dp)
            .widthIn(max = defaultMaxWidth),
    ) {
        Row(Modifier.padding(defaultPadding)) {
            if (walletAddress.isDerivedAddress()) {
                Text(
                    walletAddress.derivationIndex.toString(),
                    Modifier.align(Alignment.CenterVertically).padding(end = defaultPadding),
                    style = labelStyle(LabelStyle.HEADLINE1),
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    walletAddress.getAddressLabel(Application.texts),
                    maxLines = 1,
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    color = uiErgoColor,
                )
                MiddleEllipsisText(walletAddress.publicAddress)
            }
        }
    }

}

@Composable
fun AddAddressCard(
    onDeriveAddresses: ((Int) -> Unit)?,
) {
    AppCard(
        modifier = Modifier.padding(defaultPadding).defaultMinSize(400.dp)
            .widthIn(max = defaultMaxWidth),
    ) {

        Column(Modifier.padding(defaultPadding)) {
            Text(
                remember { Application.texts.getString(STRING_DESC_WALLET_ADDRESSES) },
                Modifier.padding(horizontal = defaultPadding * 1.5f),
                style = labelStyle(LabelStyle.BODY1),
                textAlign = TextAlign.Center,
            )

            val sliderPosition = remember { mutableStateOf(1f) }
            Slider(
                value = sliderPosition.value,
                onValueChange = { sliderPosition.value = it },
                valueRange = 1f..10f,
                modifier = Modifier.padding(
                    horizontal = defaultPadding * 1.5f,
                    vertical = defaultPadding
                ),
                steps = 10,
            )

            Button(
                { onDeriveAddresses!!.invoke(sliderPosition.value.toInt()) },
                Modifier.align(Alignment.CenterHorizontally),
                enabled = onDeriveAddresses != null,
            ) {
                Text(
                    Application.texts.getString(
                        STRING_BUTTON_ADD_ADDRESSES,
                        sliderPosition.value.toInt()
                    )
                )
            }
        }
    }

}