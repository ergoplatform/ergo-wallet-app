package org.ergoplatform.uilogic

import org.ergoplatform.ergoauth.isErgoAuthRequestUri
import org.ergoplatform.isPaymentRequestUrl
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.mosaik.getMosaikAppHttpUrl
import org.ergoplatform.mosaik.isMosaikAppUri
import org.ergoplatform.transactions.getColdSignedTxChunk
import org.ergoplatform.transactions.isColdSigningRequestChunk
import org.ergoplatform.transactions.isErgoAuthRequestChunk
import org.ergoplatform.transactions.isErgoPaySigningRequest

object MainAppUiLogic {

    fun handleRequests(
        data: String,
        fromQrCode: Boolean,
        stringProvider: StringProvider,
        navigateToChooseWalletDialog: (String) -> Unit,
        navigateToErgoPay: (String) -> Unit,
        navigateToAuthentication: (String) -> Unit,
        navigateToMosaikApp: (String) -> Unit,
        presentUserMessage: (String) -> Unit
    ) {
        if (isPaymentRequestUrl(data)) {
            navigateToChooseWalletDialog.invoke(data)
        } else if (isErgoPaySigningRequest(data)) {
            navigateToErgoPay(data)
        } else if (fromQrCode && (isColdSigningRequestChunk(data) || getColdSignedTxChunk(data) != null)) {
            // present a hint to the user to go to send funds
            presentUserMessage.invoke(stringProvider.getString(STRING_HINT_SIGNING_REQUEST))
        } else if (isErgoAuthRequestUri(data) || fromQrCode && (isErgoAuthRequestChunk(data))) {
            navigateToAuthentication.invoke(data)
        } else if (isMosaikAppUri(data)) {
            navigateToMosaikApp.invoke(getMosaikAppHttpUrl(data))
        } else if (fromQrCode && isValidErgoAddress(data)) {
            // simply an ERG address
            navigateToChooseWalletDialog.invoke(data)
        } else if (fromQrCode) {
            presentUserMessage.invoke(stringProvider.getString(STRING_ERROR_QR_CODE_CONTENT_UNKNOWN))
        }
    }
}