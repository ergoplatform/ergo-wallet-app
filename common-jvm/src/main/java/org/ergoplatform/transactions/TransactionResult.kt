package org.ergoplatform.transactions

interface TransactionResult {
    val success: Boolean
    val errorMsg: String?
}

data class SendTransactionResult(
    override val success: Boolean,
    val txId: String? = null,
    override val errorMsg: String? = null
) : TransactionResult

data class PromptSigningResult(
    override val success: Boolean,
    val serializedTx: ByteArray? = null,
    val serializedInputs: List<ByteArray>? = null,
    val address: String? = null,
    override val errorMsg: String? = null
) : TransactionResult

data class SigningResult(
    override val success: Boolean,
    val serializedTx: ByteArray? = null,
    override val errorMsg: String? = null
) : TransactionResult