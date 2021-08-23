package org.ergoplatform.android

import StageConstants
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.restapi.client.Parameters
import org.ergoplatform.wallet.mnemonic.WordList
import scala.collection.JavaConversions
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

val MNEMONIC_WORDS_COUNT = 15
val MNEMONIC_MIN_WORDS_COUNT = 12
val ERG_BASE_COST = 0
val ERG_MAX_BLOCK_COST = 1000000

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

/**
 * ERG is always formatted US-style (e.g. 1,000.00)
 */
fun formatErgsToString(ergs: Float, context: Context): String {
    return DecimalFormat(context.getString(R.string.format_erg), DecimalFormatSymbols(Locale.US)).format(ergs)
}

/**
 * fiat is formatted according to users locale, because it is his local currency
 */
fun formatFiatToString(amount: Float, currency: String, context: Context): String {
    return DecimalFormat(context.getString(R.string.format_fiat)).format(amount) +
            " " + currency.toUpperCase(Locale.getDefault())
}

/**
 * Formats token (asset) amounts, always formatted US-style
 *
 * @param formatWithPrettyReduction 1,120.00 becomes 1.1K, useful for displaying with less space
 */
fun formatTokenAmounts(
    amount: Long,
    decimals: Int,
    formatWithPrettyReduction: Boolean = false
): String {
    val valueToShow: Double = longWithDecimalsToDouble(amount, decimals)

    return if (valueToShow < 1000 || !formatWithPrettyReduction) {
        ("%." + (Math.min(5, decimals)).toString() + "f").format(Locale.US, valueToShow)
    } else {
        formatDoubleWithPrettyReduction(valueToShow)
    }
}

fun formatDoubleWithPrettyReduction(amount: Double): String {
    val suffixChars = "KMGTPE"
    val formatter = DecimalFormat("###.#", DecimalFormatSymbols(Locale.US))
    formatter.roundingMode = RoundingMode.DOWN

    return if (amount < 1000.0) formatter.format(amount)
    else {
        val exp = (ln(amount) / ln(1000.0)).toInt()
        formatter.format(amount / 1000.0.pow(exp.toDouble())) + suffixChars[exp - 1]
    }
}

fun longWithDecimalsToDouble(amount: Long, decimals: Int) =
    (amount.toDouble()) / (10.0.pow(decimals))

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
    derivedKeyIndex: Int,
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
            val prover = ctx.newProverBuilder()
                .withMnemonic(
                    SecretString.create(mnemonic),
                    SecretString.create(mnemonicPass)
                )
                .withEip3Secret(derivedKeyIndex)
                .build()

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

/**
 * Prepares and serializes a transaction to be transferred to a cold wallet (EIP19)
 */
fun prepareSerializedErgoTx(
    recipient: Address,
    amountToSend: Long,
    tokensToSend: List<ErgoToken>,
    senderAddresses: List<Address>,
    nodeApiAddress: String,
    explorerApiAddress: String
): SerializationResult {
    try {
        val ergoClient = RestApiErgoClient.create(
            nodeApiAddress,
            StageConstants.NETWORK_TYPE,
            "",
            explorerApiAddress
        )
        val reduced = ergoClient.execute { ctx: BlockchainContext ->
            val contract: ErgoContract = ErgoTreeContract(recipient.ergoAddress.script())
            val unsigned = BoxOperations.putToContractTxUnsigned(
                ctx, senderAddresses,
                contract, amountToSend, tokensToSend
            )

            val reduced = ctx.newProverBuilder().build().reduce(unsigned, ERG_BASE_COST)

            return@execute reduced
        }

        return SerializationResult(true, reduced.toBytes())
    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return SerializationResult(false, errorMsg = t.message)
    }
}

/**
 * Deserializes an unsigned transaction, signs it and serializes the signed transaction
 */
fun signSerializedErgoTx(
    serializedTx: ByteArray,
    mnemonic: String,
    mnemonicPass: String,
    derivedKeyIndex: Int
): SerializationResult {
    try {
        val coldClient = ColdErgoClient(
            StageConstants.NETWORK_TYPE,
            Parameters().maxBlockCost(ERG_MAX_BLOCK_COST)
        )

        val signedTxSerialized = coldClient.execute { ctx ->
            val prover = ctx.newProverBuilder()
                .withMnemonic(
                    SecretString.create(mnemonic),
                    SecretString.create(mnemonicPass)
                )
                .withEip3Secret(derivedKeyIndex)
                .build()
            val reducedTx = ctx.parseReducedTransaction(serializedTx)
            return@execute prover.signReduced(reducedTx, ERG_BASE_COST).toBytes()
        }
        return SerializationResult(true, signedTxSerialized)
    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return SerializationResult(false, errorMsg = t.message)
    }
}

/**
 * Sends a serialized and signed transaction
 */
fun sendSignedErgoTx(
    signedTxSerialized: ByteArray,
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
        val txId = ergoClient.execute { ctx ->
            val signedTx = ctx.parseSignedTransaction(signedTxSerialized)
            ctx.sendTransaction(signedTx)
        }

        return TransactionResult(txId.isNotEmpty(), txId)

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

data class SerializationResult(
    val success: Boolean,
    val serializedTx: ByteArray? = null,
    val errorMsg: String? = null
)