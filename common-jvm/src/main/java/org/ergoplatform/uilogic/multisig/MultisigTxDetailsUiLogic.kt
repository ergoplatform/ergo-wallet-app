package org.ergoplatform.uilogic.multisig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.MockedMultisigTransaction
import org.ergoplatform.appkit.MultisigAddress
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.MultisigTransaction
import org.ergoplatform.persistance.WalletConfig

abstract class MultisigTxDetailsUiLogic {
    val multisigTx = MutableStateFlow<MultisigTxWithExtraInfo?>(null)

    var loading = false

    fun initMultisigTx(dbId: Int, db: IAppDatabase) {
        if (multisigTx.value != null || loading)
            return

        loading = true
        coroutineScope.launch {
            val multisig = db.transactionDbProvider.loadMultisigTransaction(dbId)!!
            val multisigTxErg = MockedMultisigTransaction.fromJson(multisig.data)

            val participantList = MultisigAddress.buildFromAddress(
                Address.create("ckb945uMjo3vZ781v6mVB8bB9ANncvpgL9duZVknt8dm4CLfRX2cB7Ds4Pdwn9aF2XT3DeFPndh9ZbEhhyH7rxYb6bTdH5S9Rmqw2ytSCr")
            ) // TODO 167 multisigTxErg.transaction.signersRequired
                .participants

            multisigTx.value = MultisigTxWithExtraInfo(
                multisig,
                multisigTxErg,
                participantList = participantList.map {
                    val addrAsString = it.toString()
                    val walletDerivedAddress = db.walletDbProvider.loadWalletAddress(addrAsString)
                    val walletConfig = db.walletDbProvider.loadWalletByFirstAddress(
                        walletDerivedAddress?.walletFirstAddress ?: addrAsString
                    )
                    addrAsString to walletConfig
                },
                confirmedParticipants = listOf(participantList.last()).map { it.toString() }  // TODO 167 multisigTxErg.commitingParticipants,
            )
            loading = false
        }
    }

    abstract val coroutineScope: CoroutineScope
}

data class MultisigTxWithExtraInfo(
    val multisigTxDb: MultisigTransaction,
    val multisigTxErg: org.ergoplatform.appkit.MultisigTransaction,
    val participantList: List<Pair<String, WalletConfig?>>,
    val confirmedParticipants: List<String>
)