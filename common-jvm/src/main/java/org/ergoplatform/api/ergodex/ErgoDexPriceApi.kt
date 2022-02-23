package org.ergoplatform.api.ergodex

import org.ergoplatform.api.TokenPriceApi
import org.ergoplatform.persistance.TokenPrice
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ErgoDexPriceApi : TokenPriceApi {
    private val priceSource = "ergodex.io"
    private val baseIdErg = "0000000000000000000000000000000000000000000000000000000000000000"

    val ergoDexApi: ErgoDexApi

    init {
        val retrofit = Retrofit.Builder().baseUrl("https://api.ergodex.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        ergoDexApi = retrofit.create(ErgoDexApi::class.java)
    }


    override fun getTokenPrices(): List<TokenPrice>? {
        val swapList = ergoDexApi.swaps.execute()

        val ergBasePrices = swapList.body()
            ?.filter { it.baseId.equals(baseIdErg) && it.baseDisplayName.equals("ERG") }

        return ergBasePrices?.let {

            val hashMap = HashMap<String, ErgoDexSwap>()
            ergBasePrices.forEach {
                val otherEntry = hashMap.get(it.tokenId)

                if (otherEntry == null || otherEntry.baseVolume.value < it.baseVolume.value)
                    hashMap.put(it.tokenId, it)
            }

            hashMap.values.map {
                TokenPrice(it.tokenId, it.displayName, priceSource, it.lastPrice)
            }
        }
    }
}