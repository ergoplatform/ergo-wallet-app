package org.ergoplatform.persistance

import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount

/**
 * View on a transaction from a certain address
 */
data class AddressTransaction(
    /**
     * DB id
     */
    val id: Int,
    /**
     * address this transaction is saved and its boxes are reduced for
     */
    val address: String,
    /**
     * transaction id of this transaction
     */
    val txId: String,
    /**
     * inclusion height, when known, or [INCLUSION_HEIGHT_UNCONFIRMED] when unknown
     * (null is not used for sort order to work)
     */
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
    /**
     * purpose or other message attached to this transaction
     */
    val message: String?,
    /**
     * state this transaction is in, see [TX_STATE_SUBMITTED], [TX_STATE_WAITING], [TX_STATE_CONFIRMED_SECURE] and other constants
     */
    val state: Int,
)

/**
 * [AddressTransaction.inclusionHeight] - not confirmed. Orphaned blocks keep their former inclusion height for sorting
 */
const val INCLUSION_HEIGHT_UNCONFIRMED = Long.MAX_VALUE

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

// 2-8 reserved for future use in case automatic filtering by SQlight is needed

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

/**
 * View on a transaction token from a certain address for a certain tx id
 */
data class AddressTransactionToken(
    /**
     * DB id
     */
    val id: Int,
    /**
     * address this transaction is saved and its boxes are reduced for
     */
    val address: String,
    /**
     * transaction id of this transaction
     */
    val txId: String,
    /**
     * token id of the token changed in this tx
     */
    val tokenId: String,
    /**
     * display name of the token
     */
    val name: String,
    /**
     * see [AddressTransaction.ergAmount] on calculation
     */
    val tokenAmount: TokenAmount
)