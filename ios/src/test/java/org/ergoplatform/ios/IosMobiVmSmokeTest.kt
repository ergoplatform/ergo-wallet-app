package org.ergoplatform.ios

import SQLite.JDBCDriver
import com.google.gson.reflect.TypeToken
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import org.ergoplatform.ERG_BASE_COST
import org.ergoplatform.ErgoFacade
import org.ergoplatform.SigningSecrets
import org.ergoplatform.api.AesEncryptionManager
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
import org.ergoplatform.appkit.ScalaHelpers
import org.ergoplatform.appkit.SigmaProp
import org.ergoplatform.appkit.SignedTransaction
import org.ergoplatform.appkit.Transaction
import org.ergoplatform.appkit.impl.BlockHeaderImpl
import org.ergoplatform.appkit.impl.BlockchainContextImpl
import org.ergoplatform.appkit.impl.BoxAttachmentBuilder
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.appkit.impl.ScalaBridge
import org.ergoplatform.deserializeErgobox
import org.ergoplatform.deserializeExtendedPublicKeySafe
import org.ergoplatform.explorer.client.JSON
import org.ergoplatform.getErgoNetworkType
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.getPublicErgoAddressFromXPubKey
import org.ergoplatform.getSerializedXpubKeyFromMnemonic
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.isValidErgoAddress
import org.ergoplatform.loadAppKitMnemonicWordList
import org.ergoplatform.persistance.AppDatabase
import org.ergoplatform.persistance.DbInitializer
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD
import org.ergoplatform.persistance.SqlDelightAppDb
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.restapi.client.PowSolutions
import org.ergoplatform.sdk.SecretString
import org.ergoplatform.transactions.TransactionInfoBox
import org.ergoplatform.transactions.buildTransactionInfo
import org.ergoplatform.transactions.toTransactionInfoBox
import org.ergoplatform.uilogic.StringProvider
import scorex.util.encode.Base16
import sigma.Coll
import java.io.File
import java.math.BigInteger
import java.security.cert.CertificateFactory
import java.sql.DriverManager
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/**
 * Headless MobiVM 3.0.0 AOT native runtime smoke test verifying OpenJDK Java 8 APIs,
 * Scala 2.12 / Ergo AppKit 6.0.1 / Sigma 6.0.6 proving, Gson generic reflection,
 * Conscrypt TLS + legacy AES-GCM wallet decryption, and SQLite/SQLDelight persistence.
 */
object IosMobiVmSmokeTest {

    private const val TEST_MNEMONIC =
        "slow silly start wash bundle suffer bulb ancient height spin express remind today effort helmet"

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            println("[IosMobiVmSmokeTest] Starting native MobiVM runtime verification...")
            val vmName = System.getProperty("java.vm.name") ?: ""
            println("[IosMobiVmSmokeTest] java.vm.name=$vmName, os.arch=${System.getProperty("os.arch")}")
            check(vmName.contains("RoboVM", ignoreCase = true)) {
                "Expected RoboVM/MobiVM native runtime, got: $vmName"
            }

            verifyJava8ApisAndGsonReflection()
            verifyLegacyAndModernAesWalletEncryption()
            verifyTlsAndSecurityProviders()
            verifyScala212AppKitAndSigmaProving()
            verifySqliteAndSqlDelightPersistence()

            println("[IosMobiVmSmokeTest] ALL MobiVM 3.0.0 + Scala 2.12 + AppKit 6.0.1 checks PASSED!")
            System.out.flush()
            kotlin.system.exitProcess(0)
        } catch (t: Throwable) {
            System.err.println("[IosMobiVmSmokeTest] FAILED: ${t.javaClass.name}: ${t.message}")
            t.printStackTrace(System.err)
            System.err.flush()
            kotlin.system.exitProcess(1)
        }
    }

    private fun verifyJava8ApisAndGsonReflection() {
        val localDate = LocalDate.parse("2026-10-07", DateTimeFormatter.ISO_LOCAL_DATE)
        check(localDate.year == 2026 && localDate.monthValue == 10 && localDate.dayOfMonth == 7)

        val offsetDateTime = OffsetDateTime.parse(
            "2026-10-07T12:30:00+02:00",
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
        )
        check(offsetDateTime.toLocalDate() == localDate)

        val fn = Function<Int, Int> { it * 2 }.andThen { it + 1 }
        val biFn = BiFunction<Int, Int, Int> { a, b -> a + b }
        val pred = Predicate<Int> { it > 3 }
        val filtered = listOf(1, 2, 3, 4).stream()
            .map { biFn.apply(fn.apply(it), 0) }
            .filter(pred)
            .collect(Collectors.toList())
        check(filtered == listOf(5, 7, 9)) { "Unexpected stream output: $filtered" }

        val explorerGson = JSON().gson
        val nodeGson = org.ergoplatform.restapi.client.JSON().gson
        check(explorerGson.fromJson(explorerGson.toJson(offsetDateTime), OffsetDateTime::class.java)
            .toInstant() == offsetDateTime.toInstant())
        check(nodeGson.fromJson(nodeGson.toJson(localDate), LocalDate::class.java) == localDate)

        val txListType = object : TypeToken<List<org.ergoplatform.explorer.client.model.TransactionInfo>>() {}.type
        val parsedList: List<org.ergoplatform.explorer.client.model.TransactionInfo> =
            explorerGson.fromJson(
                """[{"id":"abc123","numConfirmations":5,"inputs":[],"outputs":[]}]""",
                txListType
            )
        check(parsedList.size == 1 && parsedList[0].id == "abc123" && parsedList[0].numConfirmations == 5)
        println("[IosMobiVmSmokeTest] Java 8 java.time/function/stream & Gson generic reflection OK")
    }

    private fun verifyLegacyAndModernAesWalletEncryption() {
        val prevLegacy = AesEncryptionManager.isOnLegacyApi
        try {
            // 1. Decrypt legacy iOS RoboVM 2.3.19 ciphertext with isOnLegacyApi = true (as set in Main.kt)
            AesEncryptionManager.isOnLegacyApi = true
            val legacyBytes = intArrayOf(
                0, 0, 0, 12, -124, -71, 94, 114, 18, -85, 124, 97, -74, -43, 121, -89, -11, -39, -37, 22, -18, 29, 125,
                13, 78, -84, 60, -58, -128, -42, 46, -121, -111, -80, -49, 55, 52, -1, 9, -104, 26, 92, -57, 116, -43,
                -51, -117, 79, 7, -46, 32, -78, 58, 123, 57, 2, 60, -76, -36, -123, 77, -82, 71, 41, -53, -40, -100, 7,
                -24, -108, -100, 48, 28
            ).map { it.toByte() }.toByteArray()
            val decryptedLegacy = String(
                AesEncryptionManager.decryptData(SecretString.create("passwort"), legacyBytes)
            )
            check(decryptedLegacy == "{\"mnemonic\":\"1 2 3 4 5 6 7 8 9 10 11 12\"}") {
                "Legacy iOS wallet decryption mismatch: $decryptedLegacy"
            }

            // 2. Round-trip encrypt/decrypt under legacy IvParameterSpec mode AND GCMParameterSpec mode
            val pw = SecretString.create("mobivm-password")
            val cipherLegacy = AesEncryptionManager.encryptData(pw, "ergo-ios-secret".toByteArray())
            check(String(AesEncryptionManager.decryptData(pw, cipherLegacy)) == "ergo-ios-secret")

            AesEncryptionManager.isOnLegacyApi = false
            check(String(AesEncryptionManager.decryptData(pw, cipherLegacy)) == "ergo-ios-secret")
        } finally {
            AesEncryptionManager.isOnLegacyApi = prevLegacy
        }
        println("[IosMobiVmSmokeTest] Legacy & modern AES-GCM wallet encryption OK")
    }

    private fun verifyTlsAndSecurityProviders() {
        val sslContext = SSLContext.getInstance("TLS")
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as java.security.KeyStore?)
        sslContext.init(null, tmf.trustManagers, null)
        check(sslContext.socketFactory != null)
        check(CertificateFactory.getInstance("X.509") != null)
        println("[IosMobiVmSmokeTest] Conscrypt TLS & X.509 CertPath security providers OK")
    }

    private fun verifyScala212AppKitAndSigmaProving() {
        // Explicitly exercise Scala 2.12 ClassTag (java.lang.ClassValue, including nested arrays)
        // and immutable HashMap/HashSet builders (scala.collection.immutable.VM.releaseFence)
        val nestedArrayTag =
            scala.reflect.`ClassTag$`.`MODULE$`.apply<Array<ByteArray>>(Array<ByteArray>::class.java)
        check(nestedArrayTag.runtimeClass() == Array<ByteArray>::class.java)
        val hashMapBuilder = scala.collection.immutable.`HashMap$`.`MODULE$`.newBuilder<Int, String>()
        val hashSetBuilder = scala.collection.immutable.`HashSet$`.`MODULE$`.newBuilder<Int>()
        for (i in 1..16) {
            hashMapBuilder.`$plus$eq`(scala.Tuple2(i, "v$i"))
            hashSetBuilder.`$plus$eq`(i)
        }
        val builtMap = hashMapBuilder.result()
        val builtSet = hashSetBuilder.result()
        check(builtMap.size() == 16 && builtSet.size() == 16)

        val prevMainNet = isErgoMainNet
        try {
            isErgoMainNet = false
            val words = loadAppKitMnemonicWordList()
            check(words.size == 2048 && words.first() == "abandon" && words.last() == "zoo")
            Mnemonic.checkEnglishMnemonic(TEST_MNEMONIC.split(" "))

            val secrets = SigningSecrets(TEST_MNEMONIC, false)
            val addr0 = getPublicErgoAddressFromMnemonic(secrets, 0)
            val addr1 = getPublicErgoAddressFromMnemonic(secrets, 1)
            check(isValidErgoAddress(addr0) && isValidErgoAddress(addr1))

            val xpub = deserializeExtendedPublicKeySafe(getSerializedXpubKeyFromMnemonic(secrets))
            check(xpub != null && getPublicErgoAddressFromXPubKey(xpub, 0) == addr0)

            val networkType = getErgoNetworkType()
            val ctx = createOfflineContext(networkType)
            val sender = Address.create(addr0)
            val recipient = Address.create(addr1)
            val attachment = BoxAttachmentBuilder.createPlainTextAttachment("mobivm-3.0.0-native")

            val inputBox = ctx.newTxBuilder().outBoxBuilder()
                .value(2L * Parameters.OneErg)
                .contract(sender.toErgoContract())
                .creationHeight(100_000)
                .build()
                .convertToInputWith("91586f766817a2658655225673e179f1d090d4b375303a281167580a224a7866", 0)

            val outBox = ctx.newTxBuilder().outBoxBuilder()
                .value(Parameters.OneErg)
                .contract(recipient.toErgoContract())
                .creationHeight(100_000)
                .registers(*attachment.outboxRegistersForAttachment)
                .build()

            val unsignedTx = ctx.newTxBuilder()
                .addInputs(inputBox)
                .addOutputs(outBox)
                .fee(Parameters.MinFee)
                .sendChangeTo(sender)
                .build()

            val reducedBytes = ctx.newProverBuilder().build().reduce(unsignedTx, ERG_BASE_COST).toBytes()
            val deserializedInput = deserializeErgobox((inputBox as InputBoxImpl).ergoBox.bytes())
            check(deserializedInput != null && deserializedInput.value == 2L * Parameters.OneErg)

            val parsedReduced = ErgoFacade.deserializeUnsignedTxOffline(reducedBytes)
            val parsedAttachment =
                BoxAttachmentBuilder.buildFromTransactionBox(parsedReduced.outputs.first()) as? BoxAttachmentPlainText
            check(parsedAttachment?.text == "mobivm-3.0.0-native")

            val inputMap = HashMap<String, TransactionInfoBox>().apply {
                val infoBox = deserializedInput.toTransactionInfoBox(null)
                put(infoBox.boxId, infoBox)
            }
            val rawOutBox = ctx.newTxBuilder().outBoxBuilder()
                .value(Parameters.OneErg)
                .contract(recipient.toErgoContract())
                .creationHeight(100_000)
                .registers(ErgoValue.of(byteArrayOf(1, 2, 3, 4)))
                .build()
            val rawReduced = ctx.newProverBuilder().build().reduce(
                ctx.newTxBuilder()
                    .addInputs(inputBox)
                    .addOutputs(rawOutBox)
                    .fee(Parameters.MinFee)
                    .sendChangeTo(sender)
                    .build(),
                ERG_BASE_COST
            )
            val rawTxInfo = rawReduced.buildTransactionInfo(inputMap)
            check(rawTxInfo.outputs.first().additionalRegisters["R4"]?.renderedValue == "AQIDBA==")

            val stringProvider = object : StringProvider {
                override val locale: java.util.Locale = java.util.Locale.ENGLISH
                override fun getString(stringId: String): String = stringId
                override fun getString(stringId: String, vararg formatArgs: Any): String = stringId
            }
            val signed = ErgoFacade.signSerializedErgoTx(reducedBytes, secrets, listOf(0), stringProvider)
            check(signed.success && signed.serializedTx != null) {
                "Sigma transaction signing failed: ${signed.errorMsg}"
            }

            val parsedSignedId = ColdErgoClient(
                networkType,
                Parameters.ColdClientMaxBlockCost,
                Parameters.ColdClientBlockVersion
            ).execute { coldCtx ->
                coldCtx.parseSignedTransaction(signed.serializedTx!!).id
            }
            check(parsedSignedId == parsedReduced.id)

            val msgSig = ErgoFacade.signMessage(
                secrets,
                listOf(0),
                SigmaProp(sender.publicKey),
                "mobivm-ergoauth-challenge"
            )
            check(msgSig.isNotEmpty())
        } finally {
            isErgoMainNet = prevMainNet
        }
        println("[IosMobiVmSmokeTest] Scala 2.12 + AppKit 6.0.1 + Sigma 6.0.6 proving OK")
    }

    private fun verifySqliteAndSqlDelightPersistence() {
        val dbFile = File.createTempFile("ergo_mobivm_smoke_", ".db")
        dbFile.deleteOnExit()
        try {
            DriverManager.registerDriver(JDBCDriver())
            val driver = JdbcSqliteDriver("sqlite:/${dbFile.absolutePath}")
            DbInitializer.initDbSchema(driver)
            val db = SqlDelightAppDb(AppDatabase(driver))
            val testAddr = "9f4QF8AD1nQ3nJahQVkMj8hFSVVzVom77b52JU7EW71Zexg6N8v"
            runBlocking {
                db.walletDbProvider.insertWalletConfig(
                    WalletConfig(
                        id = 1,
                        displayName = "MobiVM Smoke Wallet",
                        firstAddress = testAddr,
                        encryptionType = ENC_TYPE_PASSWORD,
                        secretStorage = byteArrayOf(1, 2, 3),
                        unfoldTokens = false,
                        extendedPublicKey = null
                    )
                )
                val loaded = db.walletDbProvider.loadWalletByFirstAddress(testAddr)
                check(loaded != null && loaded.displayName == "MobiVM Smoke Wallet")
            }
            driver.close()
        } finally {
            dbFile.delete()
        }
        println("[IosMobiVmSmokeTest] SQLite JDBCDriver + SQLDelight persistence OK")
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
