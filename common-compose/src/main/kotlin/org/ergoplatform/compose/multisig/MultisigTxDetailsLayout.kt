package org.ergoplatform.compose.multisig

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.compose.settings.AppCard
import org.ergoplatform.compose.settings.AppProgressIndicator
import org.ergoplatform.compose.settings.defaultMaxWidth
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.desktop.ui.ErgoAddressText
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.uilogic.STRING_MULTISIG_NO_MEMO
import org.ergoplatform.uilogic.STRING_TITLE_MULTISIGTX_PARTICIPANTS
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.multisig.MultisigTxDetailsUiLogic
import org.ergoplatform.uilogic.multisig.MultisigTxWithExtraInfo
import org.ergoplatform.uilogic.transactions.getTransactionStateString

@Composable
fun MultisigTxDetailsLayout(
    modifier: Modifier,
    multisigTxDetails: MultisigTxWithExtraInfo?,
    uiLogic: MultisigTxDetailsUiLogic,
    texts: StringProvider,
) {
    if (multisigTxDetails == null) {
        AppProgressIndicator(modifier)
    } else {

        AppCard(
            modifier.padding(defaultPadding)
                .defaultMinSize(400.dp, 200.dp)
                .widthIn(max = defaultMaxWidth)
        ) {
            MultisigTxDetails(
                Modifier.padding(defaultPadding),
                multisigTxDetails,
                uiLogic,
                texts,
            )
        }
    }
}

@Composable
private fun MultisigTxDetails(
    modifier: Modifier,
    multisigTxDetails: MultisigTxWithExtraInfo,
    uiLogic: MultisigTxDetailsUiLogic,
    texts: StringProvider,
) {

    Column(modifier) {

        MiddleEllipsisText(
            multisigTxDetails.multisigTxDb.txId,
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY1BOLD),
            color = MosaikStyleConfig.primaryLabelColor,
        )

        Text(
            multisigTxDetails.multisigTxDb.memo ?: texts.getString(STRING_MULTISIG_NO_MEMO),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY2),
        )

        Text(
            multisigTxDetails.multisigTxDb.getTransactionStateString(texts),
            Modifier.fillMaxWidth().padding(top = defaultPadding),
            textAlign = TextAlign.Center,
            style = labelStyle(LabelStyle.BODY2BOLD),
        )

        Divider(
            Modifier.padding(vertical = defaultPadding / 2),
            color = MosaikStyleConfig.secondaryLabelColor
        )

        Text(
            remember { texts.getString(STRING_TITLE_MULTISIGTX_PARTICIPANTS) },
            style = labelStyle(LabelStyle.BODY1BOLD),
        )

        // Outboxes
        Column(Modifier.padding(defaultPadding / 2)) {
            multisigTxDetails.participantList.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ErgoAddressText(
                        it.toString(),
                        Modifier.weight(1f).padding(vertical = defaultPadding / 2),
                        style = labelStyle(LabelStyle.BODY1)
                    )

                    Spacer(Modifier.size(defaultPadding))

                    Icon(
                        if (multisigTxDetails.confirmedParticipants.contains(it))
                            Icons.Outlined.CheckCircle
                        else
                            Icons.Default.MoreHoriz,
                        null,
                        tint = MosaikStyleConfig.primaryLabelColor,
                    )

                    // TODO 167 sign button
                }
            }

        }
    }

}