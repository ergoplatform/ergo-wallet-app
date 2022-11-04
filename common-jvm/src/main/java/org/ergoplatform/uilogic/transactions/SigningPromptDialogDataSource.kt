package org.ergoplatform.uilogic.transactions

import org.ergoplatform.transactions.QrCodePagesCollector
import org.ergoplatform.uilogic.STRING_BUTTON_SCAN_SIGNED_TX
import org.ergoplatform.uilogic.STRING_DESC_PROMPT_SIGNING
import org.ergoplatform.uilogic.STRING_DESC_PROMPT_SIGNING_MULTIPLE

interface SigningPromptDialogDataSource {
    val responsePagesCollector: QrCodePagesCollector
    val signingPromptData: String
    fun signingRequestToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String>
    val lastPageButtonLabel get() = STRING_BUTTON_SCAN_SIGNED_TX
    val descriptionLabel get() = STRING_DESC_PROMPT_SIGNING_MULTIPLE
    val lastPageDescriptionLabel get() = STRING_DESC_PROMPT_SIGNING
}