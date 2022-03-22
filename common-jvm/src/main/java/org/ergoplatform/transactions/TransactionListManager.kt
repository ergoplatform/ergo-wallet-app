package org.ergoplatform.transactions

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
import org.ergoplatform.ErgoApi
import org.ergoplatform.TokenAmount
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.persistance.*
import org.ergoplatform.utils.LogUtils
import java.util.concurrent.ConcurrentLinkedQueue

object TransactionListManager {
    val isDownloading: MutableStateFlow<Boolean> = MutableStateFlow(false) // TODO use it

    private val addressesToDownload = ConcurrentLinkedQueue<String>()
    private val lastAddressRefreshMs = HashMap<String, Long>()

    /**
     * enqueues address for downloading its transaction list and starts processing queue if not
     * already in operation
     */
    fun downloadTransactionListForAddress(address: String, ergoApi: ErgoApi, db: IAppDatabase) {
        if (!addressRecentlyRefreshed(address)) {
            addressesToDownload.add(address)

            startProcessQueueIfNecessary(ergoApi, db)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startProcessQueueIfNecessary(ergoApi: ErgoApi, db: IAppDatabase) {
        if (!(isDownloading.value)) {
            isDownloading.value = true
            GlobalScope.launch(Dispatchers.IO) {
                while (!addressesToDownload.isEmpty()) {
                    val address = addressesToDownload.peek()
                    if (!addressRecentlyRefreshed(address)) {
                        doDownloadTransactionList(address, ergoApi, db)
                        lastAddressRefreshMs[address] = System.currentTimeMillis()
                    }
                    addressesToDownload.remove(address)
                }
                isDownloading.value = false
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun doDownloadTransactionList(
        address: String,
        ergoApi: ErgoApi,
        db: IAppDatabase,
        loadAllTx: Boolean = false
    ) {
        try {
            val notSecurelyConfirmedTransactions = HashMap<String, AddressTransaction>()

            val heightToLoadFrom = if (loadAllTx) {
                // load all: erase all information and load everything - TODO use it
                db.transactionDbProvider.deleteAddressTransactions(address)
                0
            } else {
                // we load and refresh transactions since last securely confirmed available tx, if available
                val latestSecurelyConfirmedTx = loadAddressTransactionsUntilConfirmedSecure(
                    address,
                    notSecurelyConfirmedTransactions,
                    db
                )
                latestSecurelyConfirmedTx?.inclusionHeight
            }

            var heightSeen = Long.MAX_VALUE
            var txLoaded = 0
            var page = 0
            val txPerPage = 20

            // we also cancel for 500 loaded transactions, when not set to forced download
            while (heightToLoadFrom != null && heightSeen > heightToLoadFrom
                || heightToLoadFrom == null && txLoaded < 500 && heightSeen > 0L
            ) {
                val transactionsCall = ergoApi.getConfirmedTransactionsForAddress(
                    address,
                    txPerPage,
                    txPerPage * page
                ).execute()

                if (!transactionsCall.isSuccessful) {
                    throw IllegalStateException(transactionsCall.errorBody()!!.string())
                }

                val transactions = transactionsCall.body()!!.items
                page++
                txLoaded += transactions.size
                heightSeen = transactions.lastOrNull()?.inclusionHeight?.toLong() ?: 0L

                mergeTransactionsWithExistingAndSaveToDb(
                    address,
                    db,
                    notSecurelyConfirmedTransactions,
                    // filter the transactions, otherwise we might get duplicates for securely confirmed
                    heightToLoadFrom?.let {
                        transactions.filter { it.inclusionHeight > heightToLoadFrom }
                    } ?: transactions,
                    true
                )
            }

            // now unconfirmed
            // TODO also check mempool from node since explorer's mempool is not reliable
            val transactionsMempoolCall = ergoApi.getMempoolTransactionsForAddress(
                address,
                100,
                0
            ).execute()

            if (!transactionsMempoolCall.isSuccessful) {
                throw IllegalStateException(transactionsMempoolCall.errorBody()!!.string())
            }

            mergeTransactionsWithExistingAndSaveToDb(
                address,
                db,
                notSecurelyConfirmedTransactions,
                transactionsMempoolCall.body()!!.items,
                false
            )

            // items still in notSecurelyConfirmedTransactions: set cancelled when older than ten minutes and save to db
            notSecurelyConfirmedTransactions.values.forEach { unseenTransaction ->
                if (unseenTransaction.timestamp < System.currentTimeMillis() - 10L * 60 * 1000L) {
                    val newInclusionHeight =
                        if (unseenTransaction.inclusionHeight == INCLUSION_HEIGHT_NOT_INCLUDED) heightToLoadFrom else unseenTransaction.inclusionHeight
                    db.transactionDbProvider.insertOrUpdateAddressTransaction(
                        unseenTransaction.copy(
                            state = TX_STATE_CANCELLED,
                            inclusionHeight = newInclusionHeight ?: 0
                        )
                    )
                }
            }

        } catch (t: Throwable) {
            LogUtils.logDebug(
                "TransactionListManager",
                "Error downloading transaction list for $address",
                t
            )
        }
    }

    private suspend fun mergeTransactionsWithExistingAndSaveToDb(
        address: String,
        db: IAppDatabase,
        existingTransactions: java.util.HashMap<String, AddressTransaction>,
        newTransactions: List<TransactionInfo>,
        newConfirmed: Boolean
    ) {
        newTransactions.forEach { newTransaction ->
            val existingTransaction = existingTransactions[newTransaction.id]

            val newState =
                if (!newConfirmed) TX_STATE_WAITING else if (newTransaction.numConfirmations < CONFIRMATIONS_NUM_SECURE) TX_STATE_CONFIRMED_UNSECURE else TX_STATE_CONFIRMED_SECURE
            val newInclusionHeight =
                if (newConfirmed) newTransaction.inclusionHeight.toLong() else INCLUSION_HEIGHT_NOT_INCLUDED

            val transactionToMerge = if (existingTransaction?.state == TX_STATE_SUBMITTED) {
                // if we found a transaction that was submitted, we don't merge the information as it
                // could be incomplete (transactions in state submitted were built by the app itself)
                // instead, we delete the records and build new ones from scratch
                db.transactionDbProvider.deleteTransaction(existingTransaction.id)
                null
            } else existingTransaction

            transactionToMerge?.let {
                existingTransactions.remove(transactionToMerge.txId)
                val mergedTransaction = if (newConfirmed) {
                    // if the transaction is confirmed, use timestamp from its including block
                    transactionToMerge.copy(
                        inclusionHeight = newInclusionHeight,
                        state = newState,
                        timestamp = newTransaction.timestamp,
                    )
                } else {
                    // unconfirmed -> set state to waiting, no inclusion height. do not change timestamp as it may hold submission time
                    transactionToMerge.copy(
                        inclusionHeight = newInclusionHeight,
                        state = newState,
                    )
                }
                db.transactionDbProvider.insertOrUpdateAddressTransaction(mergedTransaction)
            } ?: run {
                // convert new transaction into db entities and save

                // first reduce the input and out information for the new transaction and hide the utxo complexities
                val reducedTxInfo = TransactionInfo(
                    newTransaction.id,
                    newTransaction.inputs,
                    newTransaction.outputs
                ).reduceBoxes()

                convertAndSaveTransactionInfoToDb(
                    reducedTxInfo,
                    address,
                    if (newConfirmed) newTransaction.timestamp else 0,
                    newInclusionHeight,
                    newState,
                    db.transactionDbProvider
                )
            }
        }
    }

    suspend fun convertAndSaveTransactionInfoToDb(
        reducedTxInfo: org.ergoplatform.transactions.TransactionInfo,
        address: String,
        timestamp: Long,
        newInclusionHeight: Long,
        newState: Int,
        db: TransactionDbProvider
    ) {
        // we now have only relevant inputs and outputs and can filter for our address
        val addressInput = reducedTxInfo.inputs.firstOrNull { it.address.equals(address) }
        val addressOutput =
            reducedTxInfo.outputs.firstOrNull { it.address.equals(address) }

        if (addressInput != null || addressOutput != null) {
            val ergAmount = ErgoAmount((addressOutput?.value ?: 0) - (addressInput?.value ?: 0))
            LogUtils.logDebug("TransactionListManager", "Saving $ergAmount ERG to $address")
            val newAddressTx = AddressTransaction(
                0,
                address,
                reducedTxInfo.id,
                newInclusionHeight,
                timestamp,
                ergAmount,
                null, // TODO parse EIP-29 message
                newState
            )

            db.insertOrUpdateAddressTransaction(newAddressTx)

            suspend fun saveTokenToDb(
                tokenList: List<AssetInstanceInfo>,
                isInput: Boolean
            ) {
                tokenList.forEach { token ->
                    db.insertOrUpdateAddressTransactionToken(
                        AddressTransactionToken(
                            0,
                            address,
                            newAddressTx.txId,
                            token.tokenId,
                            token.name ?: "",
                            TokenAmount(
                                token.amount * (if (isInput) -1 else 1),
                                token.decimals ?: 0
                            )
                        )
                    )
                }
            }

            addressInput?.assets?.let { saveTokenToDb(it, true) }
            addressOutput?.assets?.let { saveTokenToDb(it, false) }
        }
    }

    /**
     * loads all transactions until state "securely confirmed" into the referenced list and
     * returns the first securely confirmed AddressTransaction, if available
     */
    private suspend fun loadAddressTransactionsUntilConfirmedSecure(
        address: String,
        notSecurelyConfirmedTransactions: HashMap<String, AddressTransaction>,
        db: IAppDatabase
    ): AddressTransaction? {
        val pageSize = 20
        var page = 0

        var transactionList =
            db.transactionDbProvider.loadAddressTransactions(address, pageSize, page)
        var securelyConfirmedTx: AddressTransaction? = null

        while (transactionList.isNotEmpty() && securelyConfirmedTx == null) {
            transactionList.filter { it.state < TX_STATE_CONFIRMED_SECURE }
                .forEach { notSecurelyConfirmedTransactions[it.txId] = it }

            securelyConfirmedTx =
                transactionList.firstOrNull { it.state == TX_STATE_CONFIRMED_SECURE }

            if (securelyConfirmedTx == null) {
                page++
                transactionList =
                    db.transactionDbProvider.loadAddressTransactions(address, pageSize, page)
            }
        }

        return securelyConfirmedTx
    }

    private fun addressRecentlyRefreshed(address: String) =
        System.currentTimeMillis() - (lastAddressRefreshMs[address] ?: 0) <= 1000L * 30
}