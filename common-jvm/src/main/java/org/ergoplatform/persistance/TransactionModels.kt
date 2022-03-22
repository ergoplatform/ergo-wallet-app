package org.ergoplatform.persistance

import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount

/**
 * View on a transaction from a certain address
 */
data class AddressTransaction(
    val id: Int,
    val address: String,
    val txId: String,
    val inclusionHeight: Long,
    /**
     * holds block time for confirmed transactions, submission time for submitted and waiting ones, and
     * submission time or orphan block time for cancelled state
     */
    val timestamp: Long,
    /**
     * incoming erg amount, calculated by subtracting outboxes for the address from inboxes for the address
     */
    val ergAmount: ErgoAmount,
    val message: String?,
    val state: Int,
)

/**
 * [AddressTransaction.inclusionHeight] - not confirmed. Orphaned blocks keep their former inclusion height for sorting
 */
const val INCLUSION_HEIGHT_NOT_INCLUDED = Long.MAX_VALUE

/**
 * Number of confirmations needed for a transaction to be considered secure
 */
const val CONFIRMATIONS_NUM_SECURE = 72

/**
 * [AddressTransaction.state] - submitted but not seen in mempool
 */
const val TX_STATE_SUBMITTED = 0

/**
 * [AddressTransaction.state] - seen in mempool
 */
const val TX_STATE_WAITING = 1

/**
 * [AddressTransaction.state] - included in a valid block, but not [CONFIRMATIONS_NUM_SECURE] confirmations yet
 */
const val TX_STATE_CONFIRMED_UNSECURE = 9

/**
 * [AddressTransaction.state] - included in a valid block and at least [CONFIRMATIONS_NUM_SECURE] confirmations
 */
const val TX_STATE_CONFIRMED_SECURE = 10

/**
 * [AddressTransaction.state] - older five minutes, not confirmed and not in mempool
 */
const val TX_STATE_CANCELLED = -1

data class AddressTransactionToken(
    val id: Int,
    val address: String,
    val txId: String,
    val tokenId: String,
    val name: String,
    /**
     * see [AddressTransaction.ergAmount] on calculation
     */
    val tokenAmount: TokenAmount
)