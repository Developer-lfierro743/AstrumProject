package com.novusforge.astrum.core

import java.security.MessageDigest

/**
 * HashUtils - Cryptographic hashing utilities
 * Kotlin conversion: Object singleton, extension functions
 */
object HashUtils {
    
    /**
     * Compute SHA-256 hash of data
     */
    fun computeSHA256(data: ByteArray): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw IllegalStateException("SHA-256 not available", e)
        }
    }
}
