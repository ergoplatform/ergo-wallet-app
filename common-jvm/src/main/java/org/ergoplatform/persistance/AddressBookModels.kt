package org.ergoplatform.persistance

data class AddressBookEntry(
    val id: Int,
    override val label: String,
    override val address: String,
    val signedData: ByteArray?,
) : IAddressWithLabel

interface IAddressWithLabel {
    val address: String
    val label: String?
}