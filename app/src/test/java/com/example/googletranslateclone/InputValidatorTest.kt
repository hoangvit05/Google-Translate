package com.example.googletranslateclone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun testEmptyAndWhitespace() {
        assertTrue(InputValidator.isValidLength(""))
        assertTrue(InputValidator.isValidLength("   "))
    }

    @Test
    fun testCJKCharacters() {
        // Japanese: "こんにちは" (Konnichiwa) - 5 characters, 15 bytes in UTF-8
        val jaText = "こんにちは"
        assertTrue(InputValidator.isValidLength(jaText))
        
        // Chinese: "你好" (Ni Hao) - 2 characters, 6 bytes in UTF-8
        val zhText = "你好"
        assertTrue(InputValidator.isValidLength(zhText))
        
        // Repeat to exceed 500 bytes. 34 repeats of 15 bytes = 510 bytes
        val longJaText = jaText.repeat(34)
        assertFalse(InputValidator.isValidLength(longJaText))
    }

    @Test
    fun testValidLength() {
        // ASCII
        assertTrue(InputValidator.isValidLength("Hello"))

        // Vietnamese with accents
        val viText = "Xin chào "
        assertTrue(InputValidator.isValidLength(viText))

        // Ensure 500 bytes limit is handled correctly.
        val longViText = viText.repeat(51) // > 500 bytes
        assertFalse(InputValidator.isValidLength(longViText))

        val exactBytes = "A".repeat(500)
        assertTrue(InputValidator.isValidLength(exactBytes))

        val overflowBytes = "A".repeat(501)
        assertFalse(InputValidator.isValidLength(overflowBytes))
    }
}
