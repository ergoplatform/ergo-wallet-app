package org.ergoplatform

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.SecretString

private const val KEY_MNEMONIC = "mnemonic"
private const val KEY_DEPRECATED_DERIVATION = "pre1627"

data class SigningSecrets(
    val mnemonic: SecretString,
    val deprecatedDerivation: Boolean,
    val password: SecretString = SecretString.empty()
) {

    constructor(
        mnemonic: String,
        deprecatedDerivation: Boolean
    ) : this(SecretString.create(mnemonic), deprecatedDerivation)

    fun toJson(): String {
        val gson = Gson()
        val root = JsonObject()
        root.addProperty(KEY_MNEMONIC, mnemonic.toStringUnsecure())
        root.addProperty(KEY_DEPRECATED_DERIVATION, deprecatedDerivation)
        return gson.toJson(root)
    }

    companion object {
        fun fromJson(json: String): SigningSecrets? {

            try {
                val jsonTree = JsonParser().parse(json) as JsonObject

                val mnemonic =
                    SecretString.create(jsonTree.get(KEY_MNEMONIC)?.asString)
                val deprecatedDerivation =
                    jsonTree.get(KEY_DEPRECATED_DERIVATION)?.asBoolean ?: true

                return SigningSecrets(mnemonic, deprecatedDerivation)
            } catch (t: Throwable) {
                return null
            }
        }
    }
}