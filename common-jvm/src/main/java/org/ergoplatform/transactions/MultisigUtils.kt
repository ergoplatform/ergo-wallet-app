package org.ergoplatform.transactions

import org.ergoplatform.ErgoFacade
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.MockedMultisigAddress
import org.ergoplatform.appkit.MockedMultisigTransaction
import org.ergoplatform.appkit.MultisigAddress
import org.ergoplatform.persistance.MULTISIG_STATE_WAITING
import org.ergoplatform.persistance.MultisigTransaction
import org.ergoplatform.persistance.WalletConfig

object MultisigUtils {

    fun createMultisigTransaction(
        wallet: WalletConfig,
        promptSigningResult: PromptSigningResult
    ): MultisigTransaction {
        val unsignedTx = ErgoFacade.deserializeUnsignedTxOffline(promptSigningResult.serializedTx!!)

        val mst = MockedMultisigTransaction.fromTransaction(
            unsignedTx,
            MockedMultisigAddress.buildFromAddress(Address.create(wallet.firstAddress!!))
        )

        return MultisigTransaction(
            0,
            wallet.firstAddress,
            unsignedTx.id,
            System.currentTimeMillis(),
            promptSigningResult.hintMsg?.first,
            mst.toJson(),
            MULTISIG_STATE_WAITING,
        )
    }

}