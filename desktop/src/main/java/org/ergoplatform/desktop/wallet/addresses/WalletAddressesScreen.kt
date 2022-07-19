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
import org.ergoplatform.desktop.ui.secondaryButtonColors
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_BUTTON_ADD_ADDRESS
import org.ergoplatform.uilogic.STRING_BUTTON_ADD_ADDRESSES
import org.ergoplatform.uilogic.STRING_DESC_WALLET_ADDRESSES
import org.ergoplatform.uilogic.STRING_LABEL_DETAILS
import org.ergoplatform.wallet.addresses.isDerivedAddress

@Composable
fun WalletAddressesScreen(
    addressList: List<WalletAddress>,
    wallet: Wallet?,
    onDeriveAddresses: ((Int) -> Unit)?,
    onOpenDetails: (WalletAddress) -> Unit,
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
                        wallet,
                        onOpenDetails,
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
    onOpenDetails: (WalletAddress) -> Unit,
) {
    AppCard(
        modifier = Modifier.padding(defaultPadding).defaultMinSize(400.dp)
            .widthIn(max = defaultMaxWidth),
    ) {
        Column {
            AddressInfoBox(walletAddress, wallet, true, Modifier.fillMaxWidth())

            if (walletAddress.isDerivedAddress())
                Button(
                    onClick = { onOpenDetails(walletAddress) },
                    Modifier.padding(horizontal = defaultPadding)
                        .padding(bottom = defaultPadding).align(Alignment.End),
                    colors = secondaryButtonColors()
                ) {
                    Text(remember { Application.texts.getString(STRING_LABEL_DETAILS) })
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
            val sliderValue = (sliderPosition.value + .5f).toInt()
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
                { onDeriveAddresses!!.invoke(sliderValue) },
                Modifier.align(Alignment.CenterHorizontally),
                enabled = onDeriveAddresses != null,
            ) {
                Text(
                    if (sliderValue > 1)
                        Application.texts.getString(
                            STRING_BUTTON_ADD_ADDRESSES,
                            sliderValue
                        )
                    else
                        Application.texts.getString(STRING_BUTTON_ADD_ADDRESS)
                )
            }
        }
    }

}