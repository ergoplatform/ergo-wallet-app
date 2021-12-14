package org.ergoplatform.api

import org.junit.Assert.*

import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun generatePassword() {
        val length = 20
        val pw1 = PasswordGenerator.generatePassword(length)
        val pw2 = PasswordGenerator.generatePassword(length)

        assertEquals(length, pw1.length)
        assertEquals(length, pw2.length)
        assertNotEquals(pw1, pw2)
        
    }
}