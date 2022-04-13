package org.ergoplatform.desktop.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ergoplatform.ErgoAmount
import org.ergoplatform.desktop.ui.tsBody1
import org.ergoplatform.desktop.ui.tsHeadline1
import org.ergoplatform.desktop.ui.uiErgoColor
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.wallet.getBalanceForAllAddresses

@Composable
fun WalletCard(wallet: Wallet, onClicked: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(16.dp).clickable { onClicked(wallet.walletConfig.displayName!!) },
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(24.dp).defaultMinSize(400.dp, 200.dp),
            verticalArrangement = Arrangement.Center,
        ){

            Text(text = wallet.walletConfig.displayName!!,
                color = uiErgoColor,
                style = tsBody1.copy(fontWeight = FontWeight.Bold)
            )

            Text(text = ErgoAmount(wallet.getBalanceForAllAddresses()).toStringRoundToDecimals(),
                style = tsHeadline1
            )

        }
    }

}