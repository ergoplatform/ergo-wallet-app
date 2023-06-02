package org.ergoplatform

import org.ergoplatform.appkit.*
import org.ergoplatform.appkit.babelfee.BabelFeeBoxContract
import org.ergoplatform.appkit.babelfee.BabelFeeBoxState
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import kotlin.random.Random

data class BabelSwapData(
    val tokenToSwap: ErgoToken,
    val babelBox: InputBox,
    val babelAmountNanoErg: Long,
)

object BabelFees {
    fun findBabelBox(
        tokensToSend: List<ErgoToken>,
        tokenBalanceSenders: Map<String, WalletToken>,
        ctx: BlockchainContext,
        babelAmount: Long,
        texts: StringProvider,
    ): BabelSwapData {
        // construct the list of all tokens to search for babel fee
        val tokenBabelFeeSearch =
            tokensToSend + tokenBalanceSenders.values.filter { wt ->
                val tokenId = wt.tokenId ?: ""
                tokensToSend.none { it.id.toString() == tokenId }
            }.map { ErgoToken(it.tokenId ?: "", 0) }.shuffled()

        val hints = ArrayList<String>()

        tokenBabelFeeSearch.forEach { token ->
            val tokenId = token.id.toString()
            val walletToken = tokenBalanceSenders[tokenId]
            val tokenBalance = walletToken?.amount ?: 0

            // do not try to find babel fee boxes for NFTs
            if (tokenBalance > 1) {
                val toSend = token.value
                val tokenAvailable = tokenBalance - toSend

                LogUtils.logDebug(
                    "BabelFee",
                    "Checking for babel fee boxes token ${token.id} (${walletToken?.name})..."
                )

                findBabelFeeBox(
                    ctx,
                    token.id,
                    babelAmount,
                    tokenAvailable
                )?.let { babelBox ->
                    val amountNeeded =
                        babelBox.first.calcTokensToSellForErgAmount(babelAmount)

                    if (amountNeeded <= tokenAvailable) {
                        // bingo, we can use this and leave the loop
                        return BabelSwapData(
                            ErgoToken(babelBox.first.tokenId, amountNeeded),
                            babelBox.second,
                            babelAmount,
                        )
                    } else if (amountNeeded <= tokenBalance) {
                        // remember that we could use this
                        hints.add(
                            texts.getString(
                                STRING_BABELFEE_REDUCE_AMOUNT,
                                TokenAmount(
                                    amountNeeded,
                                    walletToken?.decimals ?: 0
                                ).toStringUsFormatted(),
                                walletToken?.name ?: "",
                            )
                        )
                    } else {
                        // too much, but remember to show as hint message
                        hints.add(
                            texts.getString(
                                STRING_BABELFEE_NEED_AMOUNT,
                                TokenAmount(
                                    amountNeeded,
                                    walletToken?.decimals ?: 0
                                ).toStringUsFormatted(),
                                walletToken?.name ?: "",
                                walletToken?.toTokenAmount()?.toStringUsFormatted() ?: "0"
                            )
                        )
                    }
                }
            }
        }

        val message = texts.getString(STRING_BABELFEE_HINT) + "\n" +
                if (hints.isEmpty())
                    texts.getString(STRING_BABELFEE_HINT_NONE)
                else
                    hints.joinToString("\n- ", texts.getString(STRING_BABELFEE_HINT_SOME) + "\n- ")

        throw BabelFeeSwapException(message)
    }

    private fun findBabelFeeBox(
        ctx: BlockchainContext,
        tokenId: ErgoId,
        feeAmount: Long,
        tokenAmountAvailable: Long,
    ): Pair<BabelFeeBoxState, InputBox>? {
        val loader = ExplorerAndPoolUnspentBoxesLoader().withAllowChainedTx(true)
        val maxPagesToLoadForPriceSearch = 5

        val contractForToken: ErgoContract = ErgoTreeContract(
            BabelFeeBoxContract(tokenId).ergoTree, ctx.networkType
        )
        val address = contractForToken.toAddress()
        loader.prepare(ctx, listOf(address), feeAmount, ArrayList())
        var page = 0
        var inputBoxes: List<InputBox?>? = null
        val boxesFound = ArrayList<Pair<BabelFeeBoxState, InputBox>>()

        while ((page == 0 || inputBoxes!!.isNotEmpty()) &&
            (boxesFound.isEmpty() || maxPagesToLoadForPriceSearch == 0 || page < maxPagesToLoadForPriceSearch)
        ) {
            inputBoxes = loader.loadBoxesPage(ctx, address, page)

            // find the cheapest box satisfying our fee amount needs
            for (inputBox in inputBoxes) {
                try {
                    val babelFeeBoxState = BabelFeeBoxState(inputBox)

                    if (babelFeeBoxState.valueAvailableToBuy >= feeAmount)
                        boxesFound.add(Pair(babelFeeBoxState, inputBox))
                } catch (t: Throwable) {
                    // ignore, check next
                }
            }
            page++
        }

        // sort by best price
        boxesFound.sortByDescending { it.first.pricePerToken }

        // if we have token amount available, try to select babel boxes that are suitable to cover
        // the needed fee amount with the tokens available
        val boxesToPrefer = if (tokenAmountAvailable > 0)
            boxesFound.filter { it.first.calcTokensToSellForErgAmount(feeAmount) <= tokenAmountAvailable }
        else emptyList()

        LogUtils.logDebug(
            "BabelFee",
            "${boxesFound.size} boxes found in total, ${boxesToPrefer.size} preferred"
        )

        val boxesToUse = boxesToPrefer.ifEmpty { boxesFound }

        // now we choose a random factor between 0 and 2 and cap it at min 1.0
        // to determine which price we will accept. If it is 1.0 (50% chance), we only accept
        // the best price. If it is 2.0, we accept babel fee boxes with only half of the price.
        // By this, we ensure that not only the cheapest box is used which will lead to double
        // spending problems, but also ensure that we are not paying to much (1.33 in average)
        val randomFactor = kotlin.math.max(1.0, Random.nextDouble(2.0))

        val bestPrice = boxesToUse.firstOrNull()?.first?.pricePerToken ?: 0
        val acceptedPrice = ((1.0 / randomFactor) * bestPrice).toLong()

        val acceptedBoxes = boxesToUse.filter { it.first.pricePerToken >= acceptedPrice }

        LogUtils.logDebug(
            "BabelFee",
            "Best price is $bestPrice, accepted price is $acceptedPrice, ${acceptedBoxes.size} boxes accepted"
        )

        return acceptedBoxes.randomOrNull()
    }
}

class BabelFeeSwapException(override val message: String) : Exception(message)