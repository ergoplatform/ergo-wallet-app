package org.ergoplatform.compose.addressbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.minIconSize
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.persistance.IAddressWithLabel
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.*
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getSortedDerivedAddressesList
import org.ergoplatform.wallet.sortedByDisplayName

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChooseAddressLayout(
    walletList: List<Wallet>,
    addressesList: List<AddressBookEntry>,
    onChooseEntry: (IAddressWithLabel) -> Unit,
    onEditEntry: (AddressBookEntry?) -> Unit,
    stringProvider: StringProvider,
) {
    val walletListSorted = remember(walletList) { walletList.sortedByDisplayName() }

    Column(
        Modifier.fillMaxWidth().padding(defaultPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            remember { stringProvider.getString(STRING_LABEL_OWN_ADDRESSES) },
            Modifier.padding(top = defaultPadding),
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        walletListSorted.forEach { wallet ->
            key(wallet.walletConfig.id) {
                WalletAddressesEntry(wallet, onChooseEntry, stringProvider)
            }
        }

        Divider(Modifier.padding(vertical = defaultPadding * 2))

        Text(
            remember { stringProvider.getString(STRING_LABEL_SAVED_ADDRESSES) },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        Text(
            remember { stringProvider.getString(STRING_BUTTON_ADD_ADDRESS_ENTRY) },
            Modifier.clickable { onEditEntry(null) }.padding(defaultPadding / 2),
            style = labelStyle(LabelStyle.BODY1BOLD),
        )

        addressesList.forEach { addressEntry ->
            key(addressEntry.id) {
                AddressEntry(
                    Modifier.combinedClickable(
                        onClick = { onChooseEntry(addressEntry) },
                        onLongClick = { onEditEntry(addressEntry) },
                    ).padding(defaultPadding), addressEntry
                )
            }
        }
        if (addressesList.isEmpty()) {
            Text(
                remember { stringProvider.getString(STRING_LABEL_NO_ENTRIES) },
                Modifier.padding(defaultPadding),
                style = labelStyle(LabelStyle.BODY1)
            )
        }
    }
}

@Composable
private fun AddressEntry(
    modifier: Modifier,
    addressEntry: IAddressWithLabel,
    fallBackLabel: String? = null,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        (addressEntry.label ?: fallBackLabel)?.let { label ->
            Text(
                label,
                style = labelStyle(LabelStyle.BODY1BOLD),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        MiddleEllipsisText(
            addressEntry.address,
            style = labelStyle(LabelStyle.BODY2),
        )

    }
}

@Composable
fun WalletAddressesEntry(
    wallet: Wallet,
    onChooseEntry: (IAddressWithLabel) -> Unit,
    stringProvider: StringProvider,
) {
    val expanded = remember { mutableStateOf(false) }
    val allAdresses = remember(wallet) { wallet.getSortedDerivedAddressesList() }

    if (allAdresses.size == 1) {
        AddressEntry(
            Modifier.clickable { onChooseEntry(allAdresses.first()) }.padding(defaultPadding),
            allAdresses.first(),
            wallet.walletConfig.displayName,
        )
    } else {
        Column(
            Modifier.clickable {
                expanded.value = !expanded.value
            }.fillMaxWidth().padding(defaultPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    wallet.walletConfig.displayName ?: "",
                    style = labelStyle(LabelStyle.BODY1BOLD),
                    maxLines = 1
                )
                Icon(
                    if (!expanded.value) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    null,
                    Modifier.requiredSize(minIconSize)
                )
            }

            AnimatedVisibility(expanded.value) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    allAdresses.forEach { address ->
                        key(address) {
                            AddressEntry(
                                Modifier.clickable { onChooseEntry(address) }
                                    .padding(defaultPadding / 2).fillMaxWidth(),
                                address,
                                remember(address) { address.getAddressLabel(stringProvider) }
                            )
                        }
                    }
                }
            }
        }
    }
}
