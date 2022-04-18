package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.api.ErgoExplorerApi
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.getAttachmentText
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.STRING_DESC_TRANSACTION_EXECUTION_TIME
import org.ergoplatform.uilogic.STRING_DESC_TRANSACTION_WAITING
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.millisecondsToLocalTime

abstract class TransactionInfoUiLogic {
    private val maxAddressesToShow = 200 // Mi Goreng and other airdrop fun...

    abstract val coroutineScope: CoroutineScope

    var txId: String? = null
        private set
    private var explorerTxInfo: org.ergoplatform.explorer.client.model.TransactionInfo? = null

    fun init(
        txId: String,
        address: String?, // the address is used to fetch unconfirmed transactions, see below
        ergoApi: ErgoExplorerApi
    ) {
        if (this.txId != null)
            return

        this.txId = txId
        coroutineScope.launch(Dispatchers.IO) {
            try {

                val txCall = ergoApi.getTransactionInformation(txId).execute()

                val txInfo = if (txCall.isSuccessful)
                    txCall.body()
                else if (!txCall.isSuccessful && txCall.code() == 404 && address != null) {
                    // tx might still be unconfirmed, but Explorer API has no documented endpoint to
                    // fetch unconfirmed tx by id. The undocumented API the frontend is calling
                    // does not return assets for inboxes and has a different formatting in nuances,
                    // so we trick here by fetching all unconfirmed transactions for an address
                    // and filter the result
                    val mempoolCall =
                        ergoApi.getMempoolTransactionsForAddress(address, 5, 0).execute()
                    mempoolCall.body()?.items?.firstOrNull { it.id == txId }
                } else null

                if (txInfo != null) {
                    explorerTxInfo = txInfo
                    onTransactionInformationFetched(
                        TransactionInfo(
                            txId,
                            explorerTxInfo!!.inputs,
                            explorerTxInfo!!.outputs
                        ).reduceBoxes().let {
                            TransactionInfo(
                                it.id,
                                it.inputs.take(maxAddressesToShow),
                                it.outputs.take(maxAddressesToShow)
                            )
                        }
                    )
                } else {
                    onTransactionInformationFetched(null)
                }
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "Error fetching transaction information: ${t.message}",
                    t
                )
                onTransactionInformationFetched(null)
            }
        }
    }

    /**
     * get transaction purpose by extracting attachment of first output
     */
    val transactionPurpose: String?
        get() = explorerTxInfo?.outputs?.firstOrNull()?.getAttachmentText()

    fun getTransactionExecutionState(stringProvider: StringProvider): String =
        if (explorerTxInfo?.inclusionHeight != null)
            stringProvider.getString(
                STRING_DESC_TRANSACTION_EXECUTION_TIME,
                millisecondsToLocalTime(explorerTxInfo!!.timestamp)
            )
        else
            stringProvider.getString(STRING_DESC_TRANSACTION_WAITING)

    abstract fun onTransactionInformationFetched(ti: TransactionInfo?)
}