package org.ergoplatform.android

import StageConstants
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.wallet.mnemonic.WordList
import scala.collection.JavaConversions
import java.util.*
import kotlin.math.pow

val MNEMONIC_WORDS_COUNT = 15
val MNEMONIC_MIN_WORDS_COUNT = 12

fun longWithDecimalsToDouble(amount: Long, decimals: Int) =
    (amount.toDouble()) / (10.0.pow(decimals))

fun doubleToLongWithDecimals(amount: Double, decimals: Int) =
    (amount * 10.0.pow(decimals)).toLong()

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

fun getAddressDerivationPath(index: Int): String {
    return "m/44'/429'/0'/0/$index"
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
 * loads the word list used to generate new mnemonics into a list
 */
fun loadAppKitMnemonicWordList(): List<String> {
    return JavaConversions.seqAsJavaList(WordList.load(Mnemonic.LANGUAGE_ID_ENGLISH).get().words())
}

/**
 * Create and send transaction creating a box with the given amount using parameters from the given config file.
 *
 * @param amountToSend   amount of NanoErg to put into new box
 */
fun sendErgoTx(
    recipient: Address,
    amountToSend: Long,
    tokensToSend: List<ErgoToken>,
    mnemonic: String,
    mnemonicPass: String,
    derivedKeyIndices: List<Int>,
    nodeApiAddress: String,
    explorerApiAddress: String
): TransactionResult {
    try {
        val ergoClient = RestApiErgoClient.create(
            nodeApiAddress,
            StageConstants.NETWORK_TYPE,
            "",
            explorerApiAddress
        )
        return ergoClient.execute { ctx: BlockchainContext ->
            val proverBuilder = ctx.newProverBuilder()
                .withMnemonic(
                    SecretString.create(mnemonic),
                    SecretString.create(mnemonicPass)
                )
            derivedKeyIndices.forEach {
                proverBuilder.withEip3Secret(it)
            }
            val prover = proverBuilder.build()

            val contract: ErgoContract = ErgoTreeContract(recipient.ergoAddress.script())
            val signed = BoxOperations.putToContractTx(
                ctx, prover, true,
                contract, amountToSend, tokensToSend
            )
            ctx.sendTransaction(signed)

            val txId = signed.id

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