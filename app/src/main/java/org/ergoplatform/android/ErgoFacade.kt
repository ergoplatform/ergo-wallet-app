package org.ergoplatform.android

import StageConstants
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.*
import java.text.DecimalFormat
import java.util.*

val MNEMONIC_WORDS_COUNT = 15

fun nanoErgsToErgs(nanoErgs: Long): Float {
    val microErgs = nanoErgs / (1000L * 100L)
    val ergs = microErgs.toFloat() / 10000f
    return ergs
}

fun ergsToNanoErgs(ergs: Float): Long {
    val microErgs = (ergs * 10000f).toLong()
    val nanoergs = microErgs * 100L * 1000L
    return nanoergs
}

fun formatErgsToString(ergs: Float, context: Context): String {
    return DecimalFormat(context.getString(R.string.format_erg)).format(ergs).replace(',', '.')
}

fun serializeSecrets(mnemonic: String): String {
    val gson = Gson()
    val root = JsonObject()
    root.addProperty("mnemonic", mnemonic)
    return gson.toJson(root)
}

fun deserializeSecrets(json: String): String? {

    try {
        val jsonTree = JsonParser().parse(json)

        return (jsonTree as? JsonObject)?.get("mnemonic")?.asString
    } catch (t: Throwable) {
        return null
    }
}

fun isValidErgoAddress(addressString: String): Boolean {
    if (addressString.isEmpty())
        return false

    try {
        val address = Address.create(addressString)
        return StageConstants.isValidNetworkTypeAddress(address)
    } catch (t: Throwable) {
        return false
    }

}

fun getPublicErgoAddressFromMnemonic(mnemonic: String, index: Int = 0): String {
    return Address.createEip3Address(
        index,
        StageConstants.NETWORK_TYPE,
        SecretString.create(mnemonic),
        SecretString.create("")
    ).ergoAddress.toString()
}

/**
 * Create and send transaction creating a box with the given amount using parameters from the given config file.
 *
 * @param amountToSend   amount of NanoErg to put into new box
 */
fun sendErgoTx(
    recipient: Address,
    amountToSend: Long,
    mnemonic: String,
    mnemonicPass: String,
    derivedKeyIndex: Int = 0,
    nodeApiAddress: String = StageConstants.NODE_API_ADDRESS
): TransactionResult {
    try {
        val ergoClient = RestApiErgoClient.create(nodeApiAddress, StageConstants.NETWORK_TYPE, "", StageConstants.EXPLORER_API_ADDRESS)
        return ergoClient.execute { ctx: BlockchainContext ->
            val prover = ctx.newProverBuilder()
                .withMnemonic(
                    SecretString.create(mnemonic),
                    SecretString.create(mnemonicPass)
                )
                .withEip3Secret(derivedKeyIndex)
                .build()
            val jsonTransaction = BoxOperations.send(ctx, prover, true, recipient, amountToSend)

            val jsonTree = JsonParser().parse(jsonTransaction)
            val txId = (jsonTree as JsonObject).get("id").asString

            return@execute TransactionResult(txId.isNotEmpty(), txId)
        }
    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return TransactionResult(false, errorMsg = t.message)
    }
}

data class TransactionResult(
    val success: Boolean,
    val txId: String? = null,
    val errorMsg: String? = null
)