package org.ergoplatform.transactions

import org.ergoplatform.ErgoFacade
import org.ergoplatform.SigningSecrets
import org.ergoplatform.appkit.*
import com.google.gson.Gson
import org.ergoplatform.appkit.impl.BlockHeaderImpl
import org.ergoplatform.appkit.impl.BlockchainContextImpl
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.impl.NodeInfoParameters
import org.ergoplatform.restapi.client.NodeInfo
import org.ergoplatform.sdk.SecretString
import org.ergoplatform.getErgoNetworkType
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.TestStringProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import scorex.util.encode.Base58
import sigmastate.serialization.ErgoTreeSerializer

class ErgoTreeV3OutputTest {
    // LithosDex SwapSell order template: an ErgoTree v3 script with operations that a sigma
    // version without v6 support keeps as unparsed bytes
    private val v3TreeHex =
            "1ba6041a040008cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f8179806084c444f5503" +
            "0303030402040004000e205d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d5d0100040204" +
            "00040204a09c01040204000300020002ff020006084c444f560404040406084c444f5301010101040206010104000500" +
            "0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f8" +
            "1798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303" +
            "830108cdeeac93b1a5730406084c444f5402020202d803d601b2a4730000d6027301d60373029595ed93db6508fe7303" +
            "91b1db630872017304938cb2db630872017305000173067307d809d604b2a5730800d605b2db63087204730900d606b2" +
            "db63087201730a00d6077e8c72050206d608e4c672010611d609e4c672010511d60a7e730b06d60b9972039d9c72037e" +
            "b27209730c0006720ad60c7eb27209730d0006d19683070193e5dc650cfe02730e730f027310731193c27204d0720293" +
            "8c7205018c7206019272077312927ec172040699997ec1a70672037313909c9c7e998c720602b2720873140006720b72" +
            "0c9c9a720773159a9c7e99c17201b2720873160006720a9c720b720c907eb0a57317d9010d4163d802d60f8c720d02d6" +
            "108c720d019593c2720f73189a7210c1720f72100673197202"

    private val mnemonic =
        "vast wave minute approve turn turn assault phrase ladder since initial reunion exhibit wolf horse"

    @Before
    fun setUp() {
        isErgoMainNet = true
    }

    private fun v3Tree() =
        ErgoTreeSerializer.DefaultSerializer().deserializeErgoTree(Base16.decode(v3TreeHex).get())

    // P2S address encoding from the tree bytes alone: prefix byte, script bytes, checksum
    private fun expectedP2sAddress(treeBytes: ByteArray): String {
        val prefix = (getErgoNetworkType().networkPrefix + 3).toByte()
        val content = byteArrayOf(prefix) + treeBytes
        return Base58.encode(content + Blake2b256.hash(content).copyOfRange(0, 4))
    }

    @Test
    fun addressOfV3TreeIsItsP2sAddress() {
        val tree = v3Tree()
        assertEquals(expectedP2sAddress(Base16.decode(v3TreeHex).get()), ergoTreeToAddressString(tree))
    }

    @Test
    fun addressOfParsedTreeIsUnchanged() {
        val p2pk = Address.create("9eYMpbGgBf42bCcnB2nG3wQdqPzpCCw5eB1YaWUUen9uCaW3wwm")
        assertEquals(p2pk.toString(), ergoTreeToAddressString(p2pk.ergoAddress.script()))
    }

    @Test
    fun showsAndSignsReducedTxWithV3Output() {
        val v3Tree = v3Tree()
        val ctx = BlockchainContextImpl(OfflineDataSource(), getErgoNetworkType())

        val reducedBytes = run {
            val prover = ctx.newProverBuilder()
                .withMnemonic(SecretString.create(mnemonic), SecretString.empty(), false)
                .build()
            val sender = prover.address

            val input = ctx.newTxBuilder().outBoxBuilder()
                .value(10_000_000_000L)
                .contract(sender.toErgoContract())
                .build()
                .convertToInputWith(
                    "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809", 0
                )

            val unsigned = ctx.newTxBuilder()
                .addInputs(input)
                .addOutputs(
                    ctx.newTxBuilder().outBoxBuilder()
                        .value(1_000_000_000L)
                        .contract(ErgoTreeContract(v3Tree, getErgoNetworkType()))
                        .build()
                )
                .fee(Parameters.MinFee)
                .sendChangeTo(sender)
                .build()

            prover.reduce(unsigned, 0).toBytes()
        }

        val reducedTx = ErgoFacade.deserializeUnsignedTxOffline(reducedBytes)
        val inputId = reducedTx.inputBoxesIds.first()
        val info = reducedTx.buildTransactionInfo(
            hashMapOf(inputId to TransactionInfoBox(inputId, "sender", 10_000_000_000L, emptyList()))
        )
        assertEquals(expectedP2sAddress(Base16.decode(v3TreeHex).get()), info.outputs[0].address)

        val signed = ErgoFacade.signSerializedErgoTx(
            reducedBytes, SigningSecrets(mnemonic, false), emptyList(), TestStringProvider()
        )
        assertTrue(signed.errorMsg ?: "", signed.success)
        assertTrue(Base16.encode(signed.serializedTx!!).contains(v3TreeHex))
    }
}

/**
 * Data source for building and reducing a transaction without a node: fixed protocol
 * parameters and mainnet headers 1918182 to 1918191, newest first
 */
private class OfflineDataSource : BlockchainDataSource {
    private val gson = Gson()

    override fun getParameters(): BlockchainParameters = NodeInfoParameters(
        gson.fromJson(
            """{"network": "mainnet", "parameters": {"height": 1918191, "storageFeeFactor": 1250000,
            "minValuePerByte": 360, "maxBlockSize": 1271009, "maxBlockCost": 8001091,
            "blockVersion": 4, "tokenAccessCost": 100, "inputCost": 2407, "dataInputCost": 100,
            "outputCost": 197}}""",
            NodeInfo::class.java
        )
    )

    override fun getLastBlockHeaders(count: Int, onlyRelevantParts: Boolean): List<BlockHeader> {
        val json = javaClass.getResource("/mainnet-headers.json")!!.readText()
        return gson.fromJson(
            json, Array<org.ergoplatform.restapi.client.BlockHeader>::class.java
        ).take(count).map { BlockHeaderImpl.createFromRestApi(it) }
    }

    override fun getBoxById(boxId: String, findInPool: Boolean, findInSpent: Boolean): InputBox =
        throw UnsupportedOperationException()

    override fun sendTransaction(tx: SignedTransaction): String =
        throw UnsupportedOperationException()

    override fun getUnspentBoxesFor(address: Address, offset: Int, limit: Int): List<InputBox> =
        emptyList()

    override fun getUnconfirmedUnspentBoxesFor(
        address: Address, offset: Int, limit: Int
    ): List<InputBox> = emptyList()

    override fun getUnconfirmedTransactions(offset: Int, limit: Int): List<Transaction> =
        emptyList()
}
