package org.ergoplatform.api.tokenjay

import org.ergoplatform.ErgoAmount
import org.ergoplatform.api.OkHttpSingleton
import org.ergoplatform.api.PriceImportance
import org.ergoplatform.api.TokenPriceApi
import org.ergoplatform.persistance.TokenPrice
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal

class TokenJayApiClient : TokenPriceApi {
    private val priceSource = "tokenjay.app"
    private val ageUsdTokens = listOf(
        "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", // SigUSD
        "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0" // SigRSV
    )

    private val tokenJayApi: TokenJayApi

    init {
        val retrofit = Retrofit.Builder().baseUrl("https://api.tokenjay.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpSingleton.getInstance())
            .build()
        tokenJayApi = retrofit.create(TokenJayApi::class.java)
    }

    override fun getTokenPrices(): List<Pair<TokenPrice, PriceImportance>>? {
        val priceList = tokenJayApi.getPrices().execute()

        return priceList.body()?.filter {
            (it.volumeLastDay > 0 || ageUsdTokens.contains(it.tokenId))
        }?.map {
            Pair(
                TokenPrice(
                    it.tokenId,
                    it.displayName,
                    priceSource,
                    BigDecimal.ONE.setScale(20).div(ErgoAmount(it.price).toBigDecimal())
                ),
                when {
                    it.available <= 0 -> PriceImportance.VeryLow
                    ageUsdTokens.contains(it.tokenId) -> PriceImportance.High
                    else -> PriceImportance.Normal
                }
            )
        }
    }

    fun getDetectedNodePeers(): List<TokenJayNodePeer> =
        tokenJayApi.getDetectedNodesList().execute().body()!!
}