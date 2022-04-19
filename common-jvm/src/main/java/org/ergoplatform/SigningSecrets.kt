package org.ergoplatform

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.SecretString
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.*

private const val KEY_MNEMONIC = "mnemonic"

data class SigningSecrets(
    val mnemonic: SecretString,
    val deprecatedDerivation: Boolean,
    val password: SecretString = SecretString.empty()
) {

    constructor(
        mnemonic: String,
        deprecatedDerivation: Boolean
    ) : this(SecretString.create(mnemonic), deprecatedDerivation)

    fun toBytes(): ByteArray {
        return mnemonic.toBytesSecure()
    }

    fun clearMemory() {
        mnemonic.erase()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): SigningSecrets? {
            return if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()
                && bytes.last() == '}'.code.toByte()
            ) {
                // backwards compatibility: old, unsecure json call
                fromJson(String(bytes))
            } else {
                try {
                    val mnemonic = bytes.toSecretStringSecure()
                    Arrays.fill(bytes, 0)
                    SigningSecrets(mnemonic, true)
                } catch (t: Throwable) {
                    null
                }
            }
        }

        private fun fromJson(json: String): SigningSecrets? {
            try {
                val jsonTree = JsonParser().parse(json) as JsonObject

                val mnemonic =
                    SecretString.create(jsonTree.get(KEY_MNEMONIC)?.asString)

                return SigningSecrets(mnemonic, true)
            } catch (t: Throwable) {
                return null
            }
        }
    }
}

private fun SecretString.toBytesSecure(): ByteArray {
    val charBuffer = CharBuffer.wrap(this.data)
    val byteBuffer = Charsets.UTF_8.encode(charBuffer)
    val bytes: ByteArray = Arrays.copyOfRange(
        byteBuffer.array(),
        byteBuffer.position(), byteBuffer.limit()
    )
    Arrays.fill(byteBuffer.array(), 0) // clear sensitive data
    return bytes
}

private fun ByteArray.toSecretStringSecure(): SecretString {
    val byteBuffer = ByteBuffer.wrap(this)
    val charBuffer = Charsets.UTF_8.decode(byteBuffer)
    val chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())
    Arrays.fill(charBuffer.array(), ' ')
    return SecretString.create(chars)
}