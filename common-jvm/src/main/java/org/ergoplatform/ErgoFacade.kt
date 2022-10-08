package org.ergoplatform

import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.appkit.impl.UnsignedTransactionImpl
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.restapi.client.PeersApi
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SendTransactionResult
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName
import org.ergoplatform.wallet.boxes.`ErgoBoxSerializer$`
import org.ergoplatform.wallet.mnemonic.WordList
import org.ergoplatform.wallet.secrets.ExtendedPublicKey
import scala.collection.JavaConversions
import sigmastate.interpreter.HintsBag
import sigmastate.serialization.`SigmaSerializer$`
import java.nio.charset.StandardCharsets

const val MNEMONIC_WORDS_COUNT = 15
const val MNEMONIC_MIN_WORDS_COUNT = 12
const val URL_COLD_WALLET_HELP =
    "https://github.com/ergoplatform/ergo-wallet-app/wiki/Cold-wallet"
const val URL_FORGOT_PASSWORD_HELP =
    "https://github.com/ergoplatform/ergo-wallet-app/wiki/FAQ#i-forgot-my-spending-password"

const val ERG_BASE_COST = 0
const val ERG_MAX_BLOCK_COST = 1000000
private const val MAX_NUM_INPUT_BOXES = 100

var isErgoMainNet: Boolean = true

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

fun getExplorerAddressUrl(address: String) = getExplorerWebUrl() + "en/addresses/" + address

fun getExplorerTokenUrl(tokenId: String) = getExplorerWebUrl() + "en/token/" + tokenId

fun getAddressDerivationPath(index: Int): String {
    return "m/44'/429'/0'/0/$index"
}

fun getPublicErgoAddressFromMnemonic(secrets: SigningSecrets, index: Int = 0): String {
    return Address.createEip3Address(
        index,
        getErgoNetworkType(),
        secrets.mnemonic,
        secrets.password,
        secrets.deprecatedDerivation
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

fun getSerializedXpubKeyFromMnemonic(signingSecrets: SigningSecrets) =
    Bip32Serialization.serializeExtendedPublicKeyToHex(
        JavaHelpers.seedToMasterKey(
            signingSecrets.mnemonic,
            signingSecrets.password,
            signingSecrets.deprecatedDerivation
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
    message: String?,
    amountToSend: Long,
    tokensToSend: List<ErgoToken>,
    feeAmount: Long,
    signingSecrets: SigningSecrets,
    derivedKeyIndices: List<Int>,
    prefs: PreferencesProvider,
    texts: StringProvider
): SendTransactionResult {
    try {
        val ergoClient = getRestErgoClient(prefs)
        return ergoClient.execute { ctx: BlockchainContext ->
            val prover = buildProver(ctx, signingSecrets, derivedKeyIndices)
            val unsignedTx = buildUnsignedTx(
                BoxOperations.createForEip3Prover(prover, ctx),
                recipient,
                amountToSend,
                feeAmount,
                tokensToSend,
                message
            )

            val signed = prover.sign(unsignedTx)
            ctx.sendTransaction(signed)

            val txId = signed.id

            SendTransactionResult(txId.isNotEmpty(), txId, unsignedTx)
        }
    } catch (t: Throwable) {
        LogUtils.logDebug("sendErgoTx", "Error caught", t)
        return SendTransactionResult(
            false,
            errorMsg = getErrorMessage(t, texts, amountToSend)
        )
    }
}

private fun buildUnsignedTx(
    boxOperations: BoxOperations,
    recipient: Address,
    amountToSend: Long,
    feeAmount: Long,
    tokensToSend: List<ErgoToken>,
    message: String?
): UnsignedTransaction {
    val contract: ErgoContract = recipient.toErgoContract()
    return boxOperations.withAmountToSpend(amountToSend)
        .withFeeAmount(feeAmount)
        .withMessage(message)
        .withInputBoxesLoader(
            ExplorerAndPoolUnspentBoxesLoader().withAllowChainedTx(true)
        )
        .withMaxInputBoxesToSelect(MAX_NUM_INPUT_BOXES)
        .withTokensToSpend(tokensToSend).putToContractTxUnsigned(contract)
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
            val dataSource = ctx.dataSource
            val inputs = reducedTx.inputBoxesIds.map { boxId ->
                (dataSource.getBoxByIdWithMemPool(boxId) as InputBoxImpl).ergoBox.bytes()
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
    message: String?,
    amountToSend: Long,
    tokensToSend: List<ErgoToken>,
    feeAmount: Long,
    senderAddresses: List<Address>,
    prefs: PreferencesProvider,
    texts: StringProvider
): PromptSigningResult {
    try {
        val ergoClient = getRestErgoClient(prefs)
        return ergoClient.execute { ctx: BlockchainContext ->
            val unsigned = buildUnsignedTx(
                BoxOperations.createForSenders(senderAddresses, ctx),
                recipient,
                amountToSend,
                feeAmount,
                tokensToSend,
                message
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
        return PromptSigningResult(
            false,
            errorMsg = getErrorMessage(t, texts, amountToSend)
        )
    }
}

fun deserializeUnsignedTxOffline(serializedTx: ByteArray): ReducedTransaction {
    return getColdErgoClient().execute { ctx ->
        return@execute ctx.parseReducedTransaction(serializedTx)
    }
}

/**
 * Deserializes an unsigned transaction, signs it and serializes the signed transaction
 */
fun signSerializedErgoTx(
    serializedTx: ByteArray,
    signingSecrets: SigningSecrets,
    derivedKeyIndices: List<Int>,
    texts: StringProvider
): SigningResult {
    try {
        val signedTxSerialized = getColdErgoClient().execute { ctx ->
            val prover = buildProver(ctx, signingSecrets, derivedKeyIndices)
            val reducedTx = ctx.parseReducedTransaction(serializedTx)

            return@execute prover.signReduced(reducedTx, ERG_BASE_COST).toBytes()
        }
        return SigningResult(true, signedTxSerialized)
    } catch (t: Throwable) {
        LogUtils.logDebug("signSerializedErgoTx", "Error caught", t)
        return SigningResult(false, errorMsg = getErrorMessage(t, texts))
    }
}

private fun buildProver(
    ctx: BlockchainContext,
    signingSecrets: SigningSecrets,
    derivedKeyIndices: List<Int>
): ErgoProver {
    val proverBuilder = ctx.newProverBuilder()
        .withMnemonic(
            signingSecrets.mnemonic,
            signingSecrets.password,
            signingSecrets.deprecatedDerivation
        )
    derivedKeyIndices.forEach {
        proverBuilder.withEip3Secret(it)
    }
    val prover = proverBuilder.build()
    return prover
}


fun signMessage(
    signingSecrets: SigningSecrets,
    derivedKeyIndices: List<Int>,
    sigmaBoolean: SigmaProp,
    signingMessage: String,
): ByteArray {
    return getColdErgoClient().execute { ctx ->
        val prover = buildProver(ctx, signingSecrets, derivedKeyIndices)

        prover.signMessage(
            sigmaBoolean,
            signingMessage.toByteArray(StandardCharsets.UTF_8),
            HintsBag.empty()
        )
    }
}

private fun getRestErgoClient(prefs: PreferencesProvider): ErgoClient {
    val nodeToConnectTo = prefs.prefNodeUrl
    val ergoClient = RestApiErgoClient.createWithHttpClientBuilder(
        nodeToConnectTo,
        getErgoNetworkType(),
        "",
        prefs.prefExplorerApiUrl,
        OkHttpSingleton.getInstance().newBuilder()
    )
    refreshNodeListWhenNeeded(prefs)
    return ergoClient
}

private fun refreshNodeListWhenNeeded(prefs: PreferencesProvider) {
    if (System.currentTimeMillis() - prefs.lastNodeListRefreshMs > 1000L * 60 * 60 * 24) {
        refreshNodeList(prefs)
    }
}

fun refreshNodeList(prefs: PreferencesProvider) {
    val nodeUrl = prefs.prefNodeUrl
    val peersApi = ApiServiceManager.buildRetrofitForNode(PeersApi::class.java, nodeUrl)

    try {
        val peerUrlList = peersApi.connectedPeers.execute().body()!!.map { it.restApiUrl }
        val openPeers = peerUrlList.filter { !it.isNullOrBlank() && it != nodeUrl }.distinct()
        val peersStringList =
            openPeers.sortedBy { if (it.startsWith("https://")) 0 else 1 }.take(10)

        LogUtils.logDebug("PeersApi", "Found alternative nodes to connect to: $peersStringList")
        if (peersStringList.isNotEmpty())
            prefs.knownNodesList = peersStringList
    } catch (t: Throwable) {
        LogUtils.logDebug("PeersApi", "Could not fetch connected peers of $nodeUrl")
    }
}

private fun getColdErgoClient() = ColdErgoClient(
    getErgoNetworkType(),
    ERG_MAX_BLOCK_COST
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
        return ergoClient.execute { ctx ->
            val signedTx = ctx.parseSignedTransaction(signedTxSerialized)
            val txId = ctx.sendTransaction(signedTx).trim('"')
            SendTransactionResult(txId.isNotEmpty(), txId, signedTx)
        }

    } catch (t: Throwable) {
        return SendTransactionResult(false, errorMsg = getErrorMessage(t, texts))
    }
}

fun getErrorMessage(t: Throwable, texts: StringProvider, amountToSend: Long? = null): String {
    return if (t is InputBoxesSelectionException.NotEnoughErgsException) {
        texts.getString(
            STRING_ERROR_BALANCE_ERG,
            ErgoAmount(t.balanceFound).toStringTrimTrailingZeros()
        )
    } else if (t is InputBoxesSelectionException.NotEnoughCoinsForChangeException) {
        texts.getString(
            STRING_ERROR_CHANGEBOX_AMOUNT
        )
    } else if (t is InputBoxesSelectionException.InputBoxLimitExceededException) {
        if (t.remainingTokens.isEmpty() && amountToSend != null)
            texts.getString(
                STRING_ERROR_TOO_MANY_INPUT_BOXES_LESS_ERG,
                ErgoAmount(amountToSend - t.remainingAmount).toStringTrimTrailingZeros()
            )
        else
            texts.getString(STRING_ERROR_TOO_MANY_INPUT_BOXES_GENERIC)
    } else if (t is AssertionError && t.message?.contains("Tree root should be real") == true) {
        // ProverInterpreter.scala
        texts.getString(STRING_ERROR_PROVER_CANT_SIGN)
    } else {
        t.getMessageOrName()
    }
}

fun getErgoNetworkType() = if (isErgoMainNet) NetworkType.MAINNET else NetworkType.TESTNET

fun deserializeErgobox(input: ByteArray): InputBox? {
    val r = `SigmaSerializer$`.`MODULE$`.startReader(input, 0)
    val ergoBox = `ErgoBoxSerializer$`.`MODULE$`.parse(r)
    return ergoBox?.let { InputBoxImpl(it) }
}

