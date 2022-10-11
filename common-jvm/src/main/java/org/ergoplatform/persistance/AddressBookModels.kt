package org.ergoplatform.persistance

data class AddressBookEntry(
    val id: Int,
    val label: String,
    val address: String,
    val signedData: ByteArray?,
)
