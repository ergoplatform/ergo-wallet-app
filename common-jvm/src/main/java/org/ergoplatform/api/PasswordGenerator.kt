package org.ergoplatform.api

import java.security.SecureRandom

object PasswordGenerator {

    fun generatePassword(length: Int): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val rnd = SecureRandom()
        return List(length) { chars.elementAt(rnd.nextInt(chars.size)) }.joinToString("")
    }

}