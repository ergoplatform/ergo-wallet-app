package org.ergoplatform.transactions

import org.ergoplatform.appkit.Transaction

/**
 * result of a transaction submission
 */
interface TransactionResult {
    val success: Boolean
    val errorMsg: String?
}

/**
* result of a transaction submission to a blockchain node
*/
data class SendTransactionResult(
    override val success: Boolean,
    val txId: String? = null,
    val sentTransaction: Transaction? = null,
    override val errorMsg: String? = null
) : TransactionResult

/**
 * result of a prompt to sign a transaction
 */
data class PromptSigningResult(
    override val success: Boolean,
    val serializedTx: ByteArray? = null,
    val serializedInputs: List<ByteArray>? = null,
    val address: String? = null,
    override val errorMsg: String? = null,
    val hintMsg: String? = null,
) : TransactionResult

/**
 * result of signing a transaction without submitting to the blockchain
 */
data class SigningResult(
    override val success: Boolean,
    val serializedTx: ByteArray? = null,
    override val errorMsg: String? = null
) : TransactionResult