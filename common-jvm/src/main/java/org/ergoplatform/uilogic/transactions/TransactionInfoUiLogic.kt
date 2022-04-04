package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoApi
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

    fun init(txId: String, ergoApi: ErgoApi) {
        if (this.txId != null)
            return

        this.txId = txId
        coroutineScope.launch(Dispatchers.IO) {
            try {

                val txCall = ergoApi.getTransactionInformation(txId).execute()

                if (txCall.isSuccessful) {
                    explorerTxInfo = txCall.body()
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
    val transactionPurpose: String? get() = explorerTxInfo?.outputs?.firstOrNull()?.getAttachmentText()

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