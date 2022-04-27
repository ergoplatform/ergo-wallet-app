package org.ergoplatform.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * TokenVerification API as proposed on EIP-0021 PR#61
 * https://github.com/ergoplatform/eips/pull/61
 */
interface TokenVerificationApi {
    @GET("tokens/check/{tokenId}/{tokenName}")
    fun checkToken(
        @Path("tokenId") tokenId: String,
        @Path("tokenName") tokenName: String
    ): Call<TokenCheckResponse>

}

data class GenuineToken(
    val tokenId: String,
    val tokenName: String,
    val uniqueName: Boolean,
    val issuer: String?
)

data class TokenCheckResponse(
    val genuine: Int,
    val token: GenuineToken?
)