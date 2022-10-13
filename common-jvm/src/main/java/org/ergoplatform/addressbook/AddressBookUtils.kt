package org.ergoplatform.addressbook

import org.ergoplatform.persistance.AddressBookEntry

fun List<AddressBookEntry>.sortedByLabel() = sortedBy { it.label.lowercase() }