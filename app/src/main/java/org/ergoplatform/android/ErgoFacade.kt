package org.ergoplatform.android

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.Parameters.MinFee
import org.ergoplatform.appkit.config.ErgoToolConfig
import java.io.FileNotFoundException
import java.io.Reader
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
    nodeApiAddress: String = StageConstants.NODE_API_ADDRESS
): String? {
    try {
        val ergoClient = RestApiErgoClient.create(nodeApiAddress, StageConstants.NETWORK_TYPE, "")
        return ergoClient.execute { ctx: BlockchainContext ->
            val totalToSpend = amountToSend + MinFee
            val prover = ctx.newProverBuilder()
                .withMnemonic(
                    SecretString.create(mnemonic),
                    SecretString.create(mnemonicPass)
                )
                .build()
            val unspent = ctx.getUnspentBoxesFor(prover.address)
            val boxesToSpend = BoxOperations.selectTop(unspent, totalToSpend)
            val txB = ctx.newTxBuilder()
            val newBox = txB.outBoxBuilder()
                .value(amountToSend)
                .contract(
                    ctx.compileContract(
                        ConstantsBuilder.create()
                            .item("recipientPk", recipient.publicKey)
                            .build(),
                        "{ recipientPk }"
                    )
                )
                .build()
            val tx = txB.boxesToSpend(boxesToSpend)
                .outputs(newBox)
                .fee(MinFee)
                .sendChangeTo(prover.p2PKAddress)
                .build()
            val signed = prover.sign(tx)
            val txId = ctx.sendTransaction(signed)
            return@execute txId
        }
    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return null
    }
}