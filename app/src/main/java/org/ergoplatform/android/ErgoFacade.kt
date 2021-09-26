package org.ergoplatform.android

import StageConstants
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.impl.UnsignedTransactionImpl
import org.ergoplatform.restapi.client.Parameters
import org.ergoplatform.wallet.boxes.`ErgoBoxSerializer$`
import org.ergoplatform.wallet.mnemonic.WordList
import scala.collection.JavaConversions
import sigmastate.serialization.`SigmaSerializer$`
import java.util.*

const val MNEMONIC_WORDS_COUNT = 15
const val MNEMONIC_MIN_WORDS_COUNT = 12
const val URL_COLD_WALLET_HELP =
    "https://github.com/ergoplatform/ergo-wallet-android/wiki/Cold-wallet"
val ERG_BASE_COST = 0
val ERG_MAX_BLOCK_COST = 1000000

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
): SendTransactionResult {
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

            return@execute SendTransactionResult(txId.isNotEmpty(), txId)
        }
    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return SendTransactionResult(false, errorMsg = t.message)
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
): PromptSigningResult {
    try {
        val ergoClient = RestApiErgoClient.create(
            nodeApiAddress,
            StageConstants.NETWORK_TYPE,
            "",
            explorerApiAddress
        )
        return ergoClient.execute { ctx: BlockchainContext ->
            val contract: ErgoContract = ErgoTreeContract(recipient.ergoAddress.script())
            val unsigned = BoxOperations.putToContractTxUnsigned(
                ctx, senderAddresses,
                contract, amountToSend, tokensToSend
            )

            val inputs = (unsigned as UnsignedTransactionImpl).boxesToSpend.map { box ->
                val ergoBox = box.box()
                return@map ergoBox.bytes()
            }

            val reduced = ctx.newProverBuilder().build().reduce(unsigned, ERG_BASE_COST)
            return@execute PromptSigningResult(
                true,
                reduced.toBytes(),
                inputs,
                senderAddresses.first().ergoAddress.toString()
            )
        }
    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return PromptSigningResult(false, errorMsg = t.message)
    }
}

/**
 * Deserializes an unsigned transaction, signs it and serializes the signed transaction
 */
fun signSerializedErgoTx(
    serializedTx: ByteArray,
    serializedInputs: List<ByteArray>,
    mnemonic: String,
    mnemonicPass: String,
    derivedKeyIndices: List<Int>
): SigningResult {
    try {
        val coldClient = ColdErgoClient(
            StageConstants.NETWORK_TYPE,
            Parameters().maxBlockCost(ERG_MAX_BLOCK_COST)
        )

        val signedTxSerialized = coldClient.execute { ctx ->
            val proverBuilder = ctx.newProverBuilder()
                .withMnemonic(
                    SecretString.create(mnemonic),
                    SecretString.create(mnemonicPass)
                )

            derivedKeyIndices.forEach {
                proverBuilder.withEip3Secret(it)
            }
            val prover = proverBuilder.build()
            val reducedTx = ctx.parseReducedTransaction(serializedTx)
            val inputs = reducedTx.tx.unsignedTx().inputs()

            val boxes = serializedInputs.map { input ->
                return@map deserializeErgobox(input)
            }

            // here we can check if inputs size = boxes.size && all ids match

            val outputCandidates = reducedTx.tx.unsignedTx().outputCandidates()
            return@execute prover.signReduced(reducedTx, ERG_BASE_COST).toBytes()
        }
        return SigningResult(true, signedTxSerialized)
    } catch (t: Throwable) {
        Log.e("Send", "Error signing transaction", t)
        return SigningResult(false, errorMsg = t.message)
    }
}

/**
 * Sends a serialized and signed transaction
 */
fun sendSignedErgoTx(
    signedTxSerialized: ByteArray,
    nodeApiAddress: String,
    explorerApiAddress: String
): SendTransactionResult {
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

        return SendTransactionResult(txId.isNotEmpty(), txId)

    } catch (t: Throwable) {
        Log.e("Send", "Error creating transaction", t)
        return SendTransactionResult(false, errorMsg = t.message)
    }
}

private fun deserializeErgobox(input: ByteArray): ErgoBox? {
    val r = `SigmaSerializer$`.`MODULE$`.startReader(input, 0)
    val ergoBox = `ErgoBoxSerializer$`.`MODULE$`.parse(r)
    return ergoBox
}

interface TransactionResult {
    val success: Boolean
    val errorMsg: String?
}

data class SendTransactionResult(
    override val success: Boolean,
    val txId: String? = null,
    override val errorMsg: String? = null
) : TransactionResult

data class PromptSigningResult(
    override val success: Boolean,
    val serializedTx: ByteArray? = null,
    val serializedInputs: List<ByteArray>? = null,
    val address: String? = null,
    override val errorMsg: String? = null
) : TransactionResult

data class SigningResult(
    override val success: Boolean,
    val serializedTx: ByteArray? = null,
    override val errorMsg: String? = null
) : TransactionResult