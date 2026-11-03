package org.ergoplatform

import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.BlockHeader
import org.ergoplatform.appkit.BlockchainDataSource
import org.ergoplatform.appkit.BlockchainParameters
import org.ergoplatform.appkit.BoxAttachmentPlainText
import org.ergoplatform.appkit.ColdErgoClient
import org.ergoplatform.appkit.ErgoValue
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.Mnemonic
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.appkit.SigmaProp
import org.ergoplatform.appkit.SignedTransaction
import org.ergoplatform.appkit.Transaction
import org.ergoplatform.appkit.impl.BlockHeaderImpl
import org.ergoplatform.appkit.impl.BlockchainContextImpl
import org.ergoplatform.appkit.impl.BoxAttachmentBuilder
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.appkit.impl.ScalaBridge
import org.ergoplatform.appkit.ScalaHelpers
import scorex.util.encode.Base16
import sigma.Coll
import org.ergoplatform.explorer.client.model.AdditionalRegister
import org.ergoplatform.explorer.client.model.AdditionalRegisters
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.restapi.client.PowSolutions
import org.ergoplatform.transactions.TransactionInfoBox
import org.ergoplatform.transactions.buildTransactionInfo
import org.ergoplatform.transactions.getAttachmentText
import org.ergoplatform.transactions.toTransactionInfoBox
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class ErgoFacadeAppKitTest {

    private val testMnemonic =
        "slow silly start wash bundle suffer bulb ancient height spin express remind today effort helmet"

    @Test
    fun mnemonicWordListAndKeyDerivationTest() {
        val prevMainNet = isErgoMainNet
        try {
            isErgoMainNet = true
            val words = loadAppKitMnemonicWordList()
            assertEquals(2048, words.size)
            assertEquals("abandon", words.first())
            assertEquals("zoo", words.last())

            Mnemonic.checkEnglishMnemonic(testMnemonic.split(" "))

            val secrets = SigningSecrets(testMnemonic, false)
            val addr0 = getPublicErgoAddressFromMnemonic(secrets, 0)
            val addr1 = getPublicErgoAddressFromMnemonic(secrets, 1)
            assertTrue(isValidErgoAddress(addr0))
            assertTrue(isValidErgoAddress(addr1))

            val serializedXpub = getSerializedXpubKeyFromMnemonic(secrets)
            val parsedXpub = deserializeExtendedPublicKeySafe(serializedXpub)
            assertNotNull(parsedXpub)
            assertEquals(addr0, getPublicErgoAddressFromXPubKey(parsedXpub!!, 0))
            assertEquals(addr1, getPublicErgoAddressFromXPubKey(parsedXpub, 1))
        } finally {
            isErgoMainNet = prevMainNet
        }
    }

    @Test
    fun offlineBoxSerializationAndSigmaProvingTest() {
        val prevMainNet = isErgoMainNet
        try {
            isErgoMainNet = false
            val networkType = getErgoNetworkType()
            val secrets = SigningSecrets(testMnemonic, false)
            val senderAddress = Address.create(getPublicErgoAddressFromMnemonic(secrets, 0))
            val recipientAddress = Address.create(getPublicErgoAddressFromMnemonic(secrets, 1))
            val attachment = BoxAttachmentBuilder.createPlainTextAttachment("scala-2.12-mobivm")

            val ctx = createOfflineContext(networkType)
            val dummyTxId = "91586f766817a2658655225673e179f1d090d4b375303a281167580a224a7866"
            val inputBox = ctx.newTxBuilder().outBoxBuilder()
                .value(2L * Parameters.OneErg)
                .contract(senderAddress.toErgoContract())
                .creationHeight(100_000)
                .build()
                .convertToInputWith(dummyTxId, 0)

            val outBox = ctx.newTxBuilder().outBoxBuilder()
                .value(Parameters.OneErg)
                .contract(recipientAddress.toErgoContract())
                .creationHeight(100_000)
                .registers(*attachment.outboxRegistersForAttachment)
                .build()

            val unsignedTx = ctx.newTxBuilder()
                .addInputs(inputBox)
                .addOutputs(outBox)
                .fee(Parameters.MinFee)
                .sendChangeTo(senderAddress)
                .build()

            val reducedBytes = ctx.newProverBuilder().build().reduce(unsignedTx, ERG_BASE_COST).toBytes()
            val inputBoxBytes = (inputBox as InputBoxImpl).ergoBox.bytes()

            val deserializedInput = deserializeErgobox(inputBoxBytes)
            assertNotNull(deserializedInput)
            assertEquals(2L * Parameters.OneErg, deserializedInput!!.value)

            val parsedReduced = ErgoFacade.deserializeUnsignedTxOffline(reducedBytes)
            val inputMap = HashMap<String, TransactionInfoBox>().apply {
                val infoBox = deserializedInput.toTransactionInfoBox(null)
                put(infoBox.boxId, infoBox)
            }
            val txInfo = parsedReduced.buildTransactionInfo(inputMap)
            assertEquals(1, txInfo.inputs.size)
            val parsedAttachment =
                BoxAttachmentBuilder.buildFromTransactionBox(parsedReduced.outputs.first()) as? BoxAttachmentPlainText
            assertEquals("scala-2.12-mobivm", parsedAttachment?.text)
            val explorerOut = OutputInfo().apply {
                additionalRegisters = AdditionalRegisters().apply {
                    put("R9", AdditionalRegister().apply {
                        serializedValue = parsedReduced.outputs.first().registers[5].toHex()
                    })
                }
            }
            assertEquals("scala-2.12-mobivm", explorerOut.getAttachmentText())
            assertNotNull(txInfo.outputs.first().additionalRegisters["R4"]?.renderedValue)

            // Render raw Coll<Byte> register through TransactionInfoUtils
            val rawOutBox = ctx.newTxBuilder().outBoxBuilder()
                .value(Parameters.OneErg)
                .contract(recipientAddress.toErgoContract())
                .creationHeight(100_000)
                .registers(ErgoValue.of(byteArrayOf(1, 2, 3, 4)))
                .build()
            val rawUnsigned = ctx.newTxBuilder()
                .addInputs(inputBox)
                .addOutputs(rawOutBox)
                .fee(Parameters.MinFee)
                .sendChangeTo(senderAddress)
                .build()
            val rawRegisterReduced = ctx.newProverBuilder().build().reduce(rawUnsigned, ERG_BASE_COST)
            val rawTxInfo = rawRegisterReduced.buildTransactionInfo(inputMap)
            assertEquals("AQIDBA==", rawTxInfo.outputs.first().additionalRegisters["R4"]?.renderedValue)

            val signingResult = ErgoFacade.signSerializedErgoTx(
                reducedBytes,
                secrets,
                listOf(0),
                TestStringProvider()
            )
            assertTrue(signingResult.errorMsg ?: "signing failed", signingResult.success)
            assertNotNull(signingResult.serializedTx)

            val coldClient = ColdErgoClient(
                networkType,
                Parameters.ColdClientMaxBlockCost,
                Parameters.ColdClientBlockVersion
            )
            val parsedSignedId = coldClient.execute { coldCtx ->
                coldCtx.parseSignedTransaction(signingResult.serializedTx!!).id
            }
            assertEquals(parsedReduced.id, parsedSignedId)

            val msgSignature = ErgoFacade.signMessage(
                secrets,
                listOf(0),
                SigmaProp(senderAddress.publicKey),
                "ergoauth-challenge-payload"
            )
            assertTrue(msgSignature.isNotEmpty())
            assertArrayEquals(
                deserializedInput.bytes,
                (deserializeErgobox(inputBoxBytes) as InputBoxImpl).ergoBox.bytes()
            )
        } finally {
            isErgoMainNet = prevMainNet
        }
    }

    private fun createOfflineContext(networkType: NetworkType): BlockchainContextImpl {
        val blockId = "78a76fb6c8ac11e7e9da01f2c916b82dd1220a370d7fcfe2df94caa675c21926"
        val stateRoot = "${blockId}00"
        val powSolutions = PowSolutions()
            .pk("03224c2f2388ae0741be2c50727caa49bd62654dc1f36ee72392b187b78da2c717")
            .w("0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798")
            .n("20d68047ea27a031")
            .d(BigInteger.ZERO)
        var parentIdHex = blockId
        val headersNewestFirst = (99_991..100_000).map { h ->
            val restHeader = org.ergoplatform.restapi.client.BlockHeader()
                .height(h)
                .nBits(19857408L)
                .difficulty(BigInteger.TEN)
                .id(parentIdHex)
                .parentId(parentIdHex)
                .adProofsRoot(blockId)
                .stateRoot(stateRoot)
                .transactionsRoot(blockId)
                .version(2)
                .extensionHash(blockId)
                .powSolutions(powSolutions)
                .timestamp(1_700_000_000_000L + h * 120_000L)
                .votes("000000")
            val sigmaHeader = ScalaBridge.isoBlockHeader().to(restHeader)
            @Suppress("UNCHECKED_CAST")
            val computedIdHex = Base16.encode(ScalaHelpers.collByteToByteArray(sigmaHeader.id() as Coll<Byte>))
            restHeader.id(computedIdHex)
            parentIdHex = computedIdHex
            BlockHeaderImpl(sigmaHeader, restHeader)
        }.asReversed().toList()
        val params = object : BlockchainParameters {
            override fun getNetworkType(): NetworkType = networkType
            override fun getStorageFeeFactor(): Int = 1_250_000
            override fun getMinValuePerByte(): Int = 360
            override fun getMaxBlockSize(): Int = 524_288
            override fun getTokenAccessCost(): Int = 100
            override fun getInputCost(): Int = 2_000
            override fun getDataInputCost(): Int = 100
            override fun getOutputCost(): Int = 100
            override fun getMaxBlockCost(): Int = Parameters.ColdClientMaxBlockCost
            override fun getBlockVersion(): Byte = Parameters.ColdClientBlockVersion
        }
        val dataSource = object : BlockchainDataSource {
            override fun getParameters(): BlockchainParameters = params
            override fun getLastBlockHeaders(count: Int, onlyFullHeaders: Boolean): List<BlockHeader> =
                headersNewestFirst.take(count)
            override fun getBoxById(boxId: String, findInPool: Boolean, findInSpent: Boolean): InputBox =
                throw UnsupportedOperationException()
            override fun sendTransaction(tx: SignedTransaction): String =
                throw UnsupportedOperationException()
            override fun getUnspentBoxesFor(address: Address, offset: Int, limit: Int): List<InputBox> =
                emptyList()
            override fun getUnconfirmedUnspentBoxesFor(address: Address, offset: Int, limit: Int): List<InputBox> =
                emptyList()
            override fun getUnconfirmedTransactions(offset: Int, limit: Int): List<Transaction> =
                emptyList()
        }
        return BlockchainContextImpl(dataSource, networkType)
    }
}
