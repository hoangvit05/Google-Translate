package com.example.googletranslateclone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

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
