package org.ergoplatform.api

import java.security.SecureRandom

object PasswordGenerator {

    /**
     * @return password of given length, consisting of numbers and upper and lower case latin
     *         characters determined by a secure random algorithm
     */
    fun generatePassword(length: Int): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val rnd = SecureRandom()
        return List(length) { chars.elementAt(rnd.nextInt(chars.size)) }.joinToString("")
    }

}