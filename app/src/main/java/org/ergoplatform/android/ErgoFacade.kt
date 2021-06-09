package org.ergoplatform.android

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.Parameters.MinFee
import org.ergoplatform.appkit.config.ErgoToolConfig
import java.io.FileNotFoundException
import java.io.Reader
import java.util.*

fun nanoErgsToErgs(nanoErgs: Long): Float {
   val milliErgs = nanoErgs / (1000L * 1000L)
   val ergs = milliErgs.toFloat() / 1000f
   return ergs
}

fun serializeSecrets(mnemonic: String): String {
   val gson = Gson()
   val root = JsonObject()
   root.addProperty("mnemonic", mnemonic)
   return gson.toJson(root)
}

class ErgoFacade {

   companion object {
      val MNEMONIC_WORDS_COUNT = 15

      /**
       * Create and send transaction creating a box with the given amount using parameters from the given config file.
       *
       * @param amountToSend   amount of NanoErg to put into new box
       * @param configFileName name of the configuration file relative to the current directory.
       * @return json string of the signed transaction
       */
      @Throws(FileNotFoundException::class)
      fun sendTx(amountToSend: Long, configReader: Reader): String? {
         val conf = ErgoToolConfig.load(configReader)
         val newBoxSpendingDelay = conf.parameters["newBoxSpendingDelay"]!!.toInt()
         val ownerAddress = Address.create(conf.parameters["ownerAddress"])
         val nodeConf = conf.node
         val ergoClient = RestApiErgoClient.create(nodeConf)
         return ergoClient.execute { ctx: BlockchainContext ->
            val wallet = ctx.wallet
            val totalToSpend = amountToSend + MinFee
            val boxes: Optional<List<InputBox>> =
               wallet.getUnspentBoxes(totalToSpend)
            if (!boxes.isPresent()) throw ErgoClientException(
               "Not enough coins in the wallet to pay $totalToSpend",
               null
            )
            val prover = ctx.newProverBuilder()
               .withMnemonic(
                  SecretString.create(nodeConf.wallet.mnemonic),
                  SecretString.create(nodeConf.wallet.password)
               )
               .build()
            val txB = ctx.newTxBuilder()
            val newBox = txB.outBoxBuilder()
               .value(amountToSend)
               .contract(
                  ctx.compileContract(
                     ConstantsBuilder.create()
                        .item("freezeDeadline", ctx.height + newBoxSpendingDelay)
                        .item("ownerPk", ownerAddress.publicKey)
                        .build(),
                     "{ sigmaProp(HEIGHT > freezeDeadline) && ownerPk }"
                  )
               )
               .build()
            val tx = txB.boxesToSpend(boxes.get())
               .outputs(newBox)
               .fee(MinFee)
               .sendChangeTo(prover.p2PKAddress)
               .build()
            val signed = prover.sign(tx)
            val txId = ctx.sendTransaction(signed)
            signed.toJson(true)
         }
      }

   }
}
