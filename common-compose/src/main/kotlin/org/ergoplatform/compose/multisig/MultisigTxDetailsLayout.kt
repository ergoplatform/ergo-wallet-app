package org.ergoplatform.compose.multisig

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.ergoplatform.compose.ErgoAddressText
import org.ergoplatform.compose.settings.*
import org.ergoplatform.mosaik.MiddleEllipsisText
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.labelStyle
import org.ergoplatform.mosaik.model.ui.text.LabelStyle
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.uilogic.STRING_LABEL_CONFIRM
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
    getDb: () -> IAppDatabase,
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
                getDb
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
    getDb: () -> IAppDatabase,
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
        Column(Modifier.padding(horizontal = defaultPadding / 2)) {
            multisigTxDetails.participantList.forEach { (address, wallet) ->
                val hasSigned = multisigTxDetails.confirmedParticipants.contains(address)
                Column(Modifier.padding(top = defaultPadding / 2)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ErgoAddressText(
                            address,
                            getDb,
                            texts,
                            Modifier.weight(1f),
                            style = labelStyle(LabelStyle.BODY1)
                        )

                        if (hasSigned) {
                            Spacer(Modifier.size(defaultPadding))

                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = MosaikStyleConfig.primaryLabelColor,
                            )
                        }
                    }
                    if (wallet != null && !hasSigned)
                        AppButton(onClick = {
                            // TODO 167 sign
                        }, Modifier.align(Alignment.End).padding(bottom = defaultPadding / 2)) {
                            Text(remember { texts.getString(STRING_LABEL_CONFIRM) })
                        }
                }
            }

        }
    }

}