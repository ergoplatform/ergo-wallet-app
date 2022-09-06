package org.ergoplatform

import org.junit.Test

class ApiServiceManagerTest {

    fun checkToken() {
        val apiServiceManager = ApiServiceManager.getOrInit(TestPreferencesProvider())

        val checkTokenList = listOf(
            Pair(
                "2d97cc7e16e5aac526d8b040404506e2ff96a680cf4f0fc253fe9ec1cc7bc057",
                "Σrgo Coffee Drink | Exclusive Unique NFT 1/1 | By ErgoDrinks"
            ),
            Pair(
                "9dd7fcd64bf0946545867b078b43720c4d7d26d4d8676ce8772606efeb1814be",
                "ERGREMLINS – Pokemon Collection – #3/4 – Squirtle"
            ),
            Pair(
                "a7f518585323c4d7862e5f800cbf8a5cddfbe12709acff49dc59a6e32624fb2a",
                "Gold Σnergy Drink | Unique NFT 8/8 | By ErgoDrinks"
            ),
        )

        checkTokenList.forEach { token ->
            val checkTokenCall = apiServiceManager.checkToken(token.first, token.second).execute()
            assert(checkTokenCall.isSuccessful)
        }
    }
}