package org.ergoplatform.uilogic.transactions

import org.ergoplatform.persistance.*
import org.ergoplatform.uilogic.*

data class AddressTransactionWithTokens(
    val addressTransaction: AddressTransaction,
    val tokens: List<AddressTransactionToken>
)

fun AddressTransaction.getTransactionStateString(stringProvider: StringProvider) =
    (when (state) {
        TX_STATE_SUBMITTED -> STRING_TX_STATE_SUBMITTED
        TX_STATE_CONFIRMED_SECURE -> STRING_TX_STATE_HIGH_CONFIRMATIONS
        TX_STATE_CANCELLED -> STRING_TX_STATE_CANCELLED
        TX_STATE_WAITING -> STRING_TX_STATE_WAITING
        TX_STATE_CONFIRMED_UNSECURE -> STRING_TX_STATE_LOW_CONFIRMATIONS
        else -> null
    })?.let { stringProvider.getString(it) } ?: ""

fun MultisigTransaction.getTransactionStateString(stringProvider: StringProvider) =
    (when (state) {
        MULTISIG_STATE_INVALID -> STRING_MULTISIG_TX_STATE_INVALID
        MULTISIG_STATE_WAITING -> STRING_MULTISIG_TX_STATE_WAITING
        else -> null
    })?.let { stringProvider.getString(it) } ?: ""