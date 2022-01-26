package org.ergoplatform

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.impl.UnsignedTransactionImpl
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.restapi.client.Parameters
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SendTransactionResult
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.uilogic.STRING_ERROR_BALANCE_ERG
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.boxes.`ErgoBoxSerializer$`
import org.ergoplatform.wallet.mnemonic.WordList
import scala.collection.JavaConversions
import sigmastate.serialization.`SigmaSerializer$`

const val MNEMONIC_WORDS_COUNT = 15
const val MNEMONIC_MIN_WORDS_COUNT = 12
const val URL_COLD_WALLET_HELP =
    "https://github.com/ergoplatform/ergo-wallet-app/wiki/Cold-wallet"
const val URL_FORGOT_PASSWORD_HELP =
    "https://github.com/ergoplatform/ergo-wallet-app/wiki/FAQ#i-forgot-my-spending-password"

val ERG_BASE_COST = 0
val ERG_MAX_BLOCK_COST = 1000000

var isErgoMainNet: Boolean = true

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
        return if (isErgoMainNet) address.isMainnet else !address.isMainnet
    } catch (t: Throwable) {
        return false
    }

}

fun getDefaultExplorerApiUrl() =
    if (isErgoMainNet) RestApiErgoClient.defaultMainnetExplorerUrl + "/"
    else RestApiErgoClient.defaultTestnetExplorerUrl + "/"

fun getExplorerWebUrl() =
    if (isErgoMainNet) "https://explorer.ergoplatform.com/"
    else "https://testnet.ergoplatform.com/"

fun getExplorerTxUrl(txId: String) = getExplorerWebUrl() + "en/transactions/" + txId

fun getAddressDerivationPath(index: Int): String {
    return "m/44'/429'/0'/0/$index"
}

fun getPublicErgoAddressFromMnemonic(mnemonic: String, index: Int = 0): String {
    return getPublicErgoAddressFromMnemonic(SecretString.create(mnemonic), index)
}

fun getPublicErgoAddressFromMnemonic(mnemonic: SecretString, index: Int = 0): String {
    return Address.createEip3Address(
        index,
        getErgoNetworkType(),
        mnemonic,
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
    prefs: PreferencesProvider,
    texts: StringProvider
): SendTransactionResult {
    try {
        val ergoClient = RestApiErgoClient.create(
            prefs.prefNodeUrl,
            getErgoNetworkType(),
            "",
            prefs.prefExplorerApiUrl
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
        LogUtils.logDebug("sendErgoTx", "Error caught", t)
        return SendTransactionResult(false, errorMsg = getErrorMessage(t, texts))
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
    prefs: PreferencesProvider,
    texts: StringProvider
): PromptSigningResult {
    try {
        val ergoClient = RestApiErgoClient.create(
            prefs.prefNodeUrl,
            getErgoNetworkType(),
            "",
            prefs.prefExplorerApiUrl
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
        LogUtils.logDebug("prepareSerializedErgoTx", "Error caught", t)
        return PromptSigningResult(false, errorMsg = getErrorMessage(t, texts))
    }
}

fun deserializeUnsignedTx(serializedTx: ByteArray): UnsignedErgoLikeTransaction {
    return getColdErgoClient().execute { ctx ->
        return@execute ctx.parseReducedTransaction(serializedTx).tx.unsignedTx()
    }
}

/**
 * Deserializes an unsigned transaction, signs it and serializes the signed transaction
 */
fun signSerializedErgoTx(
    serializedTx: ByteArray,
    mnemonic: String,
    mnemonicPass: String,
    derivedKeyIndices: List<Int>,
    texts: StringProvider
): SigningResult {
    try {
        val signedTxSerialized = getColdErgoClient().execute { ctx ->
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

            return@execute prover.signReduced(reducedTx, ERG_BASE_COST).toBytes()
        }
        return SigningResult(true, signedTxSerialized)
    } catch (t: Throwable) {
        LogUtils.logDebug("signSerializedErgoTx", "Error caught", t)
        return SigningResult(false, errorMsg = getErrorMessage(t, texts))
    }
}

private fun getColdErgoClient() = ColdErgoClient(
    getErgoNetworkType(),
    Parameters().maxBlockCost(ERG_MAX_BLOCK_COST)
)

/**
 * Sends a serialized and signed transaction
 */
fun sendSignedErgoTx(
    signedTxSerialized: ByteArray,
    prefs: PreferencesProvider,
    texts: StringProvider
): SendTransactionResult {
    try {
        val ergoClient = RestApiErgoClient.create(
            prefs.prefNodeUrl,
            getErgoNetworkType(),
            "",
            prefs.prefExplorerApiUrl
        )
        val txId = ergoClient.execute { ctx ->
            val signedTx = ctx.parseSignedTransaction(signedTxSerialized)
            ctx.sendTransaction(signedTx)
        }

        return SendTransactionResult(txId.isNotEmpty(), txId)

    } catch (t: Throwable) {
        return SendTransactionResult(false, errorMsg = getErrorMessage(t, texts))
    }
}

fun getErrorMessage(t: Throwable, texts: StringProvider): String? {
    return if (t is InputBoxesSelectionException.NotEnoughErgsException) {
        texts.getString(
            STRING_ERROR_BALANCE_ERG,
            ErgoAmount(t.balanceFound).toStringTrimTrailingZeros()
        )
    } else {
        t.message
    }
}

fun getErgoNetworkType() = if (isErgoMainNet) NetworkType.MAINNET else NetworkType.TESTNET

fun deserializeErgobox(input: ByteArray): ErgoBox? {
    val r = `SigmaSerializer$`.`MODULE$`.startReader(input, 0)
    val ergoBox = `ErgoBoxSerializer$`.`MODULE$`.parse(r)
    return ergoBox
}

