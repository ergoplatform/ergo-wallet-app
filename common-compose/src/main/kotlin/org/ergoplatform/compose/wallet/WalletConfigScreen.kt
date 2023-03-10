package org.ergoplatform.compose.wallet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.StateFlow
import org.ergoplatform.appkit.MultisigAddress
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.settings.secondaryButtonColors
import org.ergoplatform.desktop.ui.ErgoAddressText
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.ergoplatform.wallet.isReadOnly

@Composable
fun ColumnScope.WalletInfoSection(
    onAddAddresses: () -> Unit,
    onShowXpubKey: () -> Unit,
    walletConfig: WalletConfig,
    onShowMnemonic: () -> Unit,
    texts: StringProvider,
) {
    Text(
        remember { texts.getString(STRING_DESC_WALLET_ADDRESSES) },
        style = labelStyle(LabelStyle.BODY1)
    )
    AppButton(
        onClick = onAddAddresses,
        colors = secondaryButtonColors(),
        modifier = Modifier.align(Alignment.End).padding(top = defaultPadding / 2),
    ) {
        Text(remember { texts.getString(STRING_TITLE_WALLET_ADDRESSES) })
    }



    Text(
        remember { texts.getString(STRING_DESC_DISPLAY_XPUBKEY) },
        Modifier.padding(top = defaultPadding * 1.5f),
        style = labelStyle(LabelStyle.BODY1)
    )
    AppButton(
        onClick = onShowXpubKey,
        colors = secondaryButtonColors(),
        enabled = walletConfig.extendedPublicKey != null || !walletConfig.isReadOnly(),
        modifier = Modifier.align(Alignment.End).padding(top = defaultPadding / 2),
    ) {
        Text(remember { texts.getString(STRING_BUTTON_DISPLAY_XPUBKEY) })
    }



    Text(
        remember { texts.getString(STRING_DESC_DISPLAY_MNEMONIC) },
        Modifier.padding(top = defaultPadding * 1.5f),
        style = labelStyle(LabelStyle.BODY1)
    )
    AppButton(
        onClick = onShowMnemonic,
        colors = secondaryButtonColors(),
        enabled = !walletConfig.isReadOnly(),
        modifier = Modifier.align(Alignment.End).padding(top = defaultPadding / 2),
    ) {
        Text(remember { texts.getString(STRING_BUTTON_DISPLAY_MNEMONIC) })
    }
}

@Composable
fun ColumnScope.MultisigInfoSection(
    multiSigStateFlow: StateFlow<MultisigAddress?>,
    texts: StringProvider,
) {
    Text(
        remember { texts.getString(STRING_LABEL_MULTISIG_CONFIG) },
        style = labelStyle(LabelStyle.BODY1),
    )

    Spacer(Modifier.size(defaultPadding))
    val multisigConfig = multiSigStateFlow.collectAsState()

    if (multisigConfig.value == null) {
        AppProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    } else {
        val multisigAddress = multisigConfig.value!!

        Text(
            remember { texts.getString(STRING_LABEL_NUM_SIGNERS) },
            Modifier.align(Alignment.CenterHorizontally),
            style = labelStyle(LabelStyle.BODY1),
            textAlign = TextAlign.Center,
        )

        Text(
            multisigAddress.signersRequiredCount.toString(),
            Modifier.align(Alignment.CenterHorizontally),
            style = labelStyle(LabelStyle.HEADLINE2),
        )

        Spacer(Modifier.size(defaultPadding))

        Text(
            remember { texts.getString(STRING_LABEL_MULTISIG_PARTICIPANTS) },
            Modifier.align(Alignment.CenterHorizontally),
            style = labelStyle(LabelStyle.BODY1BOLD),
            textAlign = TextAlign.Center,
        )

        multisigAddress.participants.forEach {
            ErgoAddressText(
                it.toString(),
                style = labelStyle(LabelStyle.BODY1),
                textAlign = TextAlign.Center,
            )
        }
    }
}