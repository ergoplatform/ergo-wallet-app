package org.ergoplatform.api.tokenjay

import retrofit2.Call
import retrofit2.http.GET

interface TokenJayApi {
    @GET("tokens/prices/all")
    fun getPrices(): Call<List<TokenJayPrice>>

    @GET("peers/list")
    fun getDetectedNodesList(): Call<List<TokenJayNodePeer>>
}