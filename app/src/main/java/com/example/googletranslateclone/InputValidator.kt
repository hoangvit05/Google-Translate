package com.example.googletranslateclone

object InputValidator {
    const val MAX_BYTES = 500

    fun isValidLength(text: String): Boolean {
        return text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES
    }
}
