package org.ergoplatform.api

import okhttp3.OkHttpClient

/**
 * manages OkHttp singleton instance
 */
object OkHttpSingleton {
    @Volatile
    private var instance: OkHttpClient? = null

    fun getInstance(): OkHttpClient {
        return instance ?: synchronized(this) {
            instance ?: OkHttpClient().also { instance = it }
        }
    }
}