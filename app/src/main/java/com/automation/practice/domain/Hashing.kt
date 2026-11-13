package com.automation.practice.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash password & PIN sebelum masuk database (ARCHITECTURE.md D13). Format: `pbkdf2$iterasi$saltHex$hashHex`.
 * PBKDF2WithHmacSHA1 dipilih karena varian SHA256 baru ada di API 26, sedangkan minSdk 24.
 */
object Hashing {
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 10_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    fun hash(secret: String): String {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        return "pbkdf2$$ITERATIONS$${salt.hex()}$${derive(secret, salt, ITERATIONS).hex()}"
    }

    /** false juga untuk format tersimpan yang rusak. */
    fun verify(secret: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        return runCatching { MessageDigest.isEqual(parts[3].unhex(), derive(secret, parts[2].unhex(), iterations)) }.getOrDefault(false)
    }

    private fun derive(secret: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance(ALGORITHM).generateSecret(PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)).encoded

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun String.unhex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
