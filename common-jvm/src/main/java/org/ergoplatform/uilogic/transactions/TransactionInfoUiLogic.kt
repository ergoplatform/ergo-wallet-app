package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.persistance.AddressTransaction
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.getAttachmentText
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.transactions.toExplorerTransactionInfo
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
    var isLoading: Boolean = false
        private set
    private var explorerTxInfo: org.ergoplatform.explorer.client.model.TransactionInfo? = null
    private var localDbInfo: AddressTransaction? = null

    fun init(
        txId: String,
        address: String?, // the address is used to fetch unconfirmed transactions, see below
        ergoApi: ApiServiceManager,
        db: IAppDatabase,
        forceReload: Boolean = false,
    ) {
        if (this.txId != null && !forceReload || isLoading)
            return

        this.txId = txId
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val txInfoNode = if (ergoApi.preferNodeAsExplorer) try {
                    val blockchainTxInfo = ergoApi.getTransactionInformationNode(txId).execute()
                    if (!blockchainTxInfo.isSuccessful && blockchainTxInfo.code() == 404)
                        null // TODO unconfirmed transactions return error 404, see if we can try to find in mempool
                    else blockchainTxInfo.body()
                        ?.toExplorerTransactionInfo(ergoApi) { tokenId ->
                            TokenInfoManager.getInstance()
                                .getTokenInformation(tokenId, db.tokenDbProvider, ergoApi)
                        }

                } catch (t: Throwable) {
                    LogUtils.logDebug(
                        this.javaClass.simpleName,
                        "Error fetching tx info from node",
                        t
                    )
                    null
                } else null

                val txInfo = if (txInfoNode != null)
                    txInfoNode
                else {
                    val txCall = ergoApi.getTransactionInformation(txId).execute()

                    if (txCall.isSuccessful)
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
                }

                explorerTxInfo = txInfo
                localDbInfo = null
                isLoading = false

                if (txInfo != null) {
                    onTransactionInformationFetched(
                        TransactionInfo(
                            txId,
                            txInfo.inputs,
                            txInfo.outputs
                        ).reduceBoxes().let {
                            TransactionInfo(
                                it.id,
                                it.inputs.take(maxAddressesToShow),
                                it.outputs.take(maxAddressesToShow)
                            )
                        }
                    )
                } else if (address != null) {
                    // if we could not load the transaction from explorer, check if we have it
                    // in local DB to show at least some locally saved information
                    // to show details of cancelled transactions
                    localDbInfo = db.transactionDbProvider.loadAddressTransaction(address, txId)
                    onTransactionInformationFetched(localDbInfo?.let {
                        TransactionInfo(
                            txId,
                            emptyList(),
                            emptyList()
                        )
                    })
                } else {
                    onTransactionInformationFetched(null)
                }
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "Error fetching transaction information: ${t.message}",
                    t
                )
                explorerTxInfo = null
                localDbInfo = null
                isLoading = false
                onTransactionInformationFetched(null)
            }
        }
    }

    /**
     * get transaction purpose by extracting attachment of first output
     */
    val transactionPurpose: String?
        get() = if (localDbInfo != null) localDbInfo?.message
        else explorerTxInfo?.outputs?.map { it.getAttachmentText() }?.filterNotNull()?.firstOrNull()

    fun getTransactionExecutionState(stringProvider: StringProvider): String =
        localDbInfo?.getTransactionStateString(stringProvider)
            ?: if (explorerTxInfo?.inclusionHeight != null)
                stringProvider.getString(
                    STRING_DESC_TRANSACTION_EXECUTION_TIME,
                    millisecondsToLocalTime(explorerTxInfo!!.timestamp)
                )
            else
                stringProvider.getString(STRING_DESC_TRANSACTION_WAITING)

    fun shouldOfferReloadButton() = !isLoading &&
            explorerTxInfo == null || explorerTxInfo?.inclusionHeight == null

    abstract fun onTransactionInformationFetched(ti: TransactionInfo?)
}