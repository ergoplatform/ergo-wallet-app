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
    val timestamp: Long,
    /**
     * incoming erg amount, calculated by subtracting outboxes for the address from inboxes for the address
     */
    val ergAmount: ErgoAmount,
    val state: Int,
)

/**
 * [AddressTransaction.inclusionHeight] - not confirmed. Orphaned blocks keep their former inclusion height for sorting
 */
const val INCLUSION_HEIGHT_NOT_INCLUDED = Long.MAX_VALUE

/**
 * [AddressTransaction.state] - submitted but not seen in mempool
 */
const val TX_STATE_SUBMITTED = 0
/**
 * [AddressTransaction.state] - seen in mempool
 */
const val TX_STATE_WAITING = 1
/**
 * [AddressTransaction.state] - included in a valid block
 */
const val TX_STATE_CONFIRMED = 3
/**
 * [AddressTransaction.state] - older five minutes, not confirmed and not in mempool
 */
const val TX_STATE_CANCELLED = 4

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
