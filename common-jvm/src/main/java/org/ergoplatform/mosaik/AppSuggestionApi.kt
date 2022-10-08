package org.ergoplatform.mosaik

import retrofit2.Call
import retrofit2.http.GET

interface AppSuggestionApi {
    @GET("mosaik/develop/appsuggestions.json")
    fun getAppSuggestions(): Call<List<MosaikAppSuggestion>>
}

data class MosaikAppSuggestion(val appName: String, val appDescription: String, val appUrl: String)