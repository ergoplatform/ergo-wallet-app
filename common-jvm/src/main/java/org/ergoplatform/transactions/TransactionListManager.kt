package org.ergoplatform.transactions

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.persistance.*
import org.ergoplatform.utils.LogUtils
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

/**
 * This singleton manages the in app transaction history lists by ensuring that only one download
 * is launched at a time and automatic downloads aren't launched too frequently. It holds the logic
 * for updating transaction lists.
 *
 * Transaction history lists are updated as follows per address:
 * - the most recent securely confirmed history list entry is loaded from the DB. Securely confirmed
 *   means no orphaning or other changes are expected any more
 * - transaction history for this address is fetched from Ergo Explorer in chunks until the block
 *   height of the most recent securely confirmed history list entry is reached
 * - mempool is also fetched
 * - the fetched tx history list from Explorer is merged with the not securely confirmed history
 *   list from our DB: transactions are added or updated or, if not known in the blockchain any more,
 *   set to cancelled
 *
 * The algorithm is more meant to be fast and to save bandwidth than to be 100% accurate. For
 * addresses with a high throughput rate, transactions might get lost when loading chunks from
 * Explorer is cancelled midway. However, the saved data can always be fixed by starting a
 * complete download which wipes all address data before starting to download the complete history.
 */
object TransactionListManager {
    val isDownloading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val downloadProgress: MutableStateFlow<Int> = MutableStateFlow(0)
    val downloadAddress: MutableStateFlow<String?> = MutableStateFlow(null)

    private val addressesToDownload = ConcurrentLinkedQueue<String>()
    private val lastAddressRefreshMs = HashMap<String, Long>()

    /**
     * enqueues address for downloading its transaction list and starts processing queue if not
     * already in operation
     */
    fun downloadTransactionListForAddress(
        address: String,
        ergoApi: ApiServiceManager,
        db: IAppDatabase
    ) {
        if (!addressRecentlyRefreshed(address)) {
            addressesToDownload.add(address)

            startProcessQueueIfNecessary(ergoApi, db)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startProcessQueueIfNecessary(ergoApi: ApiServiceManager, db: IAppDatabase) {
        if (!(isDownloading.value)) {
            setDownloadState(true)
            GlobalScope.launch(Dispatchers.IO) {
                while (!addressesToDownload.isEmpty()) {
                    val address = addressesToDownload.peek()
                    if (!addressRecentlyRefreshed(address)) {
                        doDownloadTransactionList(address, ergoApi, db)
                    }
                    addressesToDownload.remove(address)
                }
                setDownloadState(false)
            }
        }
    }

    /**
     * When no download is in progress, this will wipe all data saved for this address and attempt
     * to download tx history list until block height 0
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun startDownloadAllAddressTransactions(
        address: String,
        ergoApi: ApiServiceManager,
        db: IAppDatabase
    ): Boolean {
        return if (!(isDownloading.value)) {
            setDownloadState(true)
            GlobalScope.launch(Dispatchers.IO) {
                doDownloadTransactionList(address, ergoApi, db, true)
                setDownloadState(false)
            }
            true
        } else false
    }

    /**
     * determines how many transactions must be fetched from Explorer, fetches it and calls
     * helper methods to merge the downloaded and current data
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun doDownloadTransactionList(
        address: String,
        ergoApi: ApiServiceManager,
        db: IAppDatabase,
        loadAllTx: Boolean = false
    ) {
        try {
            val notSecurelyConfirmedTransactions = HashMap<String, AddressTransaction>()

            val heightToLoadFrom = if (loadAllTx) {
                // load all: erase all information and load everything
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

            // lowest block height we've downloaded
            var heightSeen = Long.MAX_VALUE
            // highest block height we've seen an executed tx
            var highestExecuted = (heightToLoadFrom ?: 0)
            var txLoaded = 0
            var page = 0
            val txPerPage = 20
            downloadAddress.value = address

            // we also cancel for 500 loaded transactions, when not set to forced download
            while (heightToLoadFrom != null && heightSeen > heightToLoadFrom
                || heightToLoadFrom == null && txLoaded < 500 && heightSeen > 0L
            ) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "Fetching address $address transactions page $page"
                )
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
                downloadProgress.value = txLoaded
                heightSeen = transactions.lastOrNull()?.inclusionHeight?.toLong() ?: 0L
                highestExecuted =
                    max(
                        highestExecuted,
                        transactions.firstOrNull()?.inclusionHeight?.toLong() ?: 0L
                    )

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
            val transactionsMempoolCall = ergoApi.getMempoolTransactionsForAddress(
                address,
                100,
                0
            ).execute()

            val mempoolTransactionsExplorer = if (transactionsMempoolCall.isSuccessful) {
                transactionsMempoolCall.body()!!.items
            } else emptyList()

            mergeTransactionsWithExistingAndSaveToDb(
                address,
                db,
                notSecurelyConfirmedTransactions,
                mempoolTransactionsExplorer,
                false
            )

            if (notSecurelyConfirmedTransactions.isNotEmpty()) {
                // explorer's mempool is not reliable: sometimes it returns blank, so also check
                // mempool from node and add the transaction ids so that we don't show transactions
                // as cancelled that are still waiting
                val unconfirmedTransactionsNodeCall =
                    ergoApi.getUnconfirmedTransactions(1000).execute()

                val unconfirmedTxIdsNode = if (unconfirmedTransactionsNodeCall.isSuccessful) {
                    unconfirmedTransactionsNodeCall.body()!!.map { it.id }
                } else emptyList()

                // items still in notSecurelyConfirmedTransactions: set cancelled when older than ten minutes and save to db
                notSecurelyConfirmedTransactions.values.forEach { unseenTransaction ->
                    if (unseenTransaction.timestamp < System.currentTimeMillis() - 10L * 60 * 1000L &&
                        !unconfirmedTxIdsNode.contains(unseenTransaction.txId)
                    ) {
                        val newInclusionHeight =
                            if (unseenTransaction.inclusionHeight == INCLUSION_HEIGHT_UNCONFIRMED) highestExecuted + 1
                            else unseenTransaction.inclusionHeight
                        db.transactionDbProvider.insertOrUpdateAddressTransaction(
                            unseenTransaction.copy(
                                state = TX_STATE_CANCELLED,
                                inclusionHeight = newInclusionHeight
                            )
                        )
                    }
                }
            }

            // mark as downloaded so on attempt to redownload it again within the next few seconds
            lastAddressRefreshMs[address] = System.currentTimeMillis()
        } catch (t: Throwable) {
            LogUtils.logDebug(
                "TransactionListManager",
                "Error downloading transaction list for $address: ${t.message}",
                t
            )
        }
    }

    /**
     * merges existing data with downloaded data, updates transaction state and (in case
     * transaction was not completely known before) the transaction data as well
     */
    private suspend fun mergeTransactionsWithExistingAndSaveToDb(
        address: String,
        db: IAppDatabase,
        existingTransactions: java.util.HashMap<String, AddressTransaction>,
        newTransactions: List<TransactionInfo>,
        newConfirmed: Boolean
    ) {
        newTransactions.forEach { newTransaction ->
            val existingTransaction = existingTransactions.remove(newTransaction.id)

            val newState =
                if (!newConfirmed) TX_STATE_WAITING else if (newTransaction.numConfirmations < CONFIRMATIONS_NUM_SECURE) TX_STATE_CONFIRMED_UNSECURE else TX_STATE_CONFIRMED_SECURE
            val newInclusionHeight =
                if (newConfirmed) newTransaction.inclusionHeight.toLong() else INCLUSION_HEIGHT_UNCONFIRMED

            val transactionToMerge = if (
                existingTransaction?.state == TX_STATE_SUBMITTED ||
                existingTransaction?.state == TX_STATE_CANCELLED
            ) {
                // if we found a transaction that was submitted, we don't merge the information as it
                // could be incomplete (transactions in state submitted were built by the app itself)
                // instead, we delete the records and build new ones from scratch
                db.transactionDbProvider.deleteTransaction(existingTransaction.id)
                null
            } else existingTransaction

            transactionToMerge?.let {
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

                val txInfo = TransactionInfo(
                    newTransaction.id,
                    newTransaction.inputs,
                    newTransaction.outputs
                )

                convertAndSaveTransactionInfoToDb(
                    txInfo,
                    address,
                    if (newConfirmed) newTransaction.timestamp else (existingTransaction?.timestamp
                        ?: System.currentTimeMillis()),
                    newInclusionHeight,
                    newState,
                    db.transactionDbProvider
                )
            }
        }
    }

    /**
     * converts given transaction data into address transaction data by reducing inputs and outputs
     * into a single amount per token
     */
    suspend fun convertAndSaveTransactionInfoToDb(
        txInfo: org.ergoplatform.transactions.TransactionInfo,
        address: String,
        timestamp: Long,
        newInclusionHeight: Long,
        newState: Int,
        db: TransactionDbProvider
    ) {
        val reducedTxInfo = txInfo.reduceBoxes()

        // we now have only relevant inputs and outputs and can filter for our address
        val addressInput = reducedTxInfo.inputs.firstOrNull { it.address.equals(address) }
        val addressOutput =
            reducedTxInfo.outputs.firstOrNull { it.address.equals(address) }

        if (addressInput != null || addressOutput != null) {
            val ergAmount = ErgoAmount((addressOutput?.value ?: 0) - (addressInput?.value ?: 0))
            val firstTextAttachmentToAddress =
                txInfo.outputs.firstOrNull { it.address.equals(address) }?.getAttachmentText()
                    ?: txInfo.outputs.firstOrNull()?.getAttachmentText()
            LogUtils.logDebug(this.javaClass.simpleName, "Saving ${reducedTxInfo.id} for $address")
            val newAddressTx = AddressTransaction(
                0,
                address,
                reducedTxInfo.id,
                newInclusionHeight,
                timestamp,
                ergAmount,
                firstTextAttachmentToAddress,
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

    private fun setDownloadState(isDownloading: Boolean) {
        downloadProgress.value = 0
        this.isDownloading.value = isDownloading
        downloadAddress.value = null
    }

    private fun addressRecentlyRefreshed(address: String) =
        System.currentTimeMillis() - (lastAddressRefreshMs[address]
            ?: 0) <= 1000L * 30 // 30 seconds
}