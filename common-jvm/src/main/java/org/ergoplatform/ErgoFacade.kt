package org.ergoplatform

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.*
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.restapi.client.Parameters
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SendTransactionResult
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.transactions.getInputBoxesIds
import org.ergoplatform.uilogic.STRING_ERROR_BALANCE_ERG
import org.ergoplatform.uilogic.STRING_ERROR_PROVER_CANT_SIGN
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.boxes.`ErgoBoxSerializer$`
import org.ergoplatform.wallet.mnemonic.WordList
import org.ergoplatform.wallet.secrets.ExtendedPublicKey
import scala.collection.JavaConversions
import sigmastate.serialization.`SigmaSerializer$`

const val MNEMONIC_WORDS_COUNT = 15
const val MNEMONIC_MIN_WORDS_COUNT = 12
const val URL_COLD_WALLET_HELP =
    "https://github.com/ergoplatform/ergo-wallet-app/wiki/Cold-wallet"
const val URL_FORGOT_PASSWORD_HELP =
    "https://github.com/ergoplatform/ergo-wallet-app/wiki/FAQ#i-forgot-my-spending-password"

const val ERG_BASE_COST = 0
const val ERG_MAX_BLOCK_COST = 1000000

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

fun getExplorerTokenUrl(tokenId: String) = getExplorerWebUrl() + "en/token/" + tokenId

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

fun getPublicErgoAddressFromXPubKey(xPubKey: ExtendedPublicKey, index: Int = 0): String {
    return Address.createEip3Address(
        index,
        getErgoNetworkType(),
        xPubKey
    ).ergoAddress.toString()
}

fun deserializeExtendedPublicKeySafe(serializedKey: String) = try {
    Bip32Serialization.parseExtendedPublicKeyFromHex(serializedKey, getErgoNetworkType())
} catch (t: Throwable) {
    null
}

fun getSerializedXpubKeyFromMnemonic(mnemonic: String) =
    Bip32Serialization.serializeExtendedPublicKeyToHex(
        JavaHelpers.seedToMasterKey(
            SecretString.create(mnemonic),
            SecretString.empty()
        ), getErgoNetworkType()
    )

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
        val ergoClient = getRestErgoClient(prefs)
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
            val signed = BoxOperations.createForEip3Prover(prover).withAmountToSpend(amountToSend)
                .withInputBoxesLoader(ExplorerAndPoolUnspentBoxesLoader().withAllowChainedTx(true))
                .withTokensToSpend(tokensToSend).putToContractTx(
                    ctx, contract
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

fun buildPromptSigningResultFromErgoPayRequest(
    serializedTx: ByteArray,
    senderAddress: String,
    prefs: PreferencesProvider,
    texts: StringProvider
): PromptSigningResult {

    try {
        return getRestErgoClient(prefs).execute { ctx ->
            val reducedTx = ctx.parseReducedTransaction(serializedTx)
            val inputs =
                ctx.getBoxesById(*reducedTx.tx.unsignedTx().getInputBoxesIds().toTypedArray())
                    .map { inputBox ->
                        (inputBox as InputBoxImpl).ergoBox.bytes()
                    }

            return@execute PromptSigningResult(
                true, serializedTx,
                inputs, senderAddress
            )
        }
    } catch (t: Throwable) {
        return PromptSigningResult(false, errorMsg = getErrorMessage(t, texts))
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
        val ergoClient = getRestErgoClient(prefs)
        return ergoClient.execute { ctx: BlockchainContext ->
            val contract: ErgoContract = ErgoTreeContract(recipient.ergoAddress.script())
            val unsigned = BoxOperations.createForSenders(senderAddresses).withAmountToSpend(amountToSend)
                .withInputBoxesLoader(ExplorerAndPoolUnspentBoxesLoader().withAllowChainedTx(true))
                .withTokensToSpend(tokensToSend).putToContractTxUnsigned(ctx, contract)

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

fun deserializeUnsignedTxOffline(serializedTx: ByteArray): UnsignedErgoLikeTransaction {
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

private fun getRestErgoClient(prefs: PreferencesProvider) =
    RestApiErgoClient.create(
        prefs.prefNodeUrl,
        getErgoNetworkType(),
        "",
        prefs.prefExplorerApiUrl
    )

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
        val ergoClient = getRestErgoClient(prefs)
        val txId = ergoClient.execute { ctx ->
            val signedTx = ctx.parseSignedTransaction(signedTxSerialized)
            ctx.sendTransaction(signedTx).trim('"')
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
    } else if (t is AssertionError && t.message?.contains("Tree root should be real") == true) {
        // ProverInterpreter.scala
        texts.getString(STRING_ERROR_PROVER_CANT_SIGN)
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

