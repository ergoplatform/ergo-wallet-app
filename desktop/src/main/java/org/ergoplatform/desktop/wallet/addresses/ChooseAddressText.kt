package org.ergoplatform.desktop.wallet.addresses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.ergoplatform.Application
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.STRING_LABEL_ALL_ADDRESSES
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getNumOfAddresses

@Composable
fun ChooseAddressButton(
    walletAddress: WalletAddress?,
    wallet: Wallet? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.clickable(onClick = onClick)) {
        Text(
            remember(walletAddress) {
                walletAddress?.getAddressLabel(Application.texts)
                    ?: Application.texts.getString(
                        STRING_LABEL_ALL_ADDRESSES,
                        wallet?.getNumOfAddresses() ?: 0
                    )
            },
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = uiErgoColor,
            maxLines = 1,
        )

        Icon(Icons.Default.ExpandMore, null, Modifier.align(Alignment.CenterVertically))
    }
}