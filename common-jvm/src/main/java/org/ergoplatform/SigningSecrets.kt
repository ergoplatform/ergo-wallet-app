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
        val bytes = mnemonic.toBytesSecure()
        return if (deprecatedDerivation) bytes else {
            val bytesWithPrefix = ByteArray(bytes.size + 1) { i ->
                if (i == 0) prefixNewMethod else bytes[i - 1]
            }
            Arrays.fill(bytes, 0)
            bytesWithPrefix
        }
    }

    fun clearMemory() {
        mnemonic.erase()
    }

    companion object {
        // if the byte array starts with a tab (code 9), the new derivation logic should be used
        private const val prefixNewMethod: Byte = 9

        fun fromBytes(bytes: ByteArray): SigningSecrets? {
            return if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()
                && bytes.last() == '}'.code.toByte()
            ) {
                // backwards compatibility: old, unsecure json call
                fromJson(String(bytes))
            } else {
                try {
                    if (bytes[0] == prefixNewMethod) {
                        val mnemonicBytes = ByteArray(bytes.size - 1) { i ->
                            bytes[i + 1]
                        }
                        val mnemonic = mnemonicBytes.toSecretStringSecure()
                        Arrays.fill(bytes, 0)
                        Arrays.fill(mnemonicBytes, 0)
                        SigningSecrets(mnemonic, false)
                    } else {
                        val mnemonic = bytes.toSecretStringSecure()
                        Arrays.fill(bytes, 0)
                        SigningSecrets(mnemonic, true)
                    }
                } catch (t: Throwable) {
                    Arrays.fill(bytes, 0)
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